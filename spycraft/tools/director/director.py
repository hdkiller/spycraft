#!/usr/bin/env python3
"""Send one local request to an explicitly enabled SpyCraft filming client."""
import argparse
import json
import os
from pathlib import Path
import time
import uuid

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--game-dir', type=Path, required=True)
parser.add_argument('--timeout', type=float, default=60)
parser.add_argument('action', choices=['create_world', 'open_world', 'quit', 'prepare', 'take', 'status', 'screenshot', 'disconnect', 'export_replay'])
parser.add_argument('--world')
parser.add_argument('--replay', type=Path)
parser.add_argument('--take', type=Path)
parser.add_argument('--save', type=Path)
args = parser.parse_args()
directory = args.game_dir / 'spycraft-director'
if not (directory / 'ready.json').exists():
    parser.error('Director not enabled: launch with -Dspycraft.director=true')
request_id = uuid.uuid4().hex[:12]
command = {'id': request_id, 'action': args.action}
if args.action == 'open_world':
    if not args.world:
        parser.error('open_world needs --world')
    command['world'] = args.world
if args.action == 'export_replay':
    if not args.replay or not args.take:
        parser.error('export_replay needs --replay and --take')
    command.update(replay=str(args.replay.resolve()), take=json.loads(args.take.read_text()))
request = directory / 'request.json'
lock = directory / 'writer.lock'
try:
    lock_fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
except FileExistsError:
    parser.error('Another director command is in progress')
try:
    os.close(lock_fd)
    if request.exists():
        parser.error('A previous request is still pending')
    pending = directory / f'request-{request_id}.tmp'
    pending.write_text(json.dumps(command))
    pending.replace(request)
    response = directory / f'response-{request_id}.json'
    deadline = time.monotonic() + args.timeout
    while not response.exists():
        if time.monotonic() > deadline:
            raise SystemExit(f'Timed out; request {request_id} may still complete. Check {response}')
        time.sleep(0.1)
    answer = json.loads(response.read_text())
    if args.save:
        args.save.parent.mkdir(parents=True, exist_ok=True)
        args.save.write_text(json.dumps(answer, indent=2))
    print(json.dumps(answer, indent=2))
    if answer.get('state') == 'error':
        raise SystemExit(1)
finally:
    lock.unlink(missing_ok=True)
