# Coding New Items, Blocks & Superpowers

Here is a practical recipe book for creating fun new features with Erik.

---

## Recipe 1: Adding a Custom Food Item (e.g. Erik's Super Pizza)

Kids love custom food that gives ridiculous superpower potion effects (Speed, Jump Boost, Night Vision).

### 1. Define the Food Properties
In `src/main/java/com/erikcraft/item/ModItems.java`:

```java
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {
    // 1. Define the Food: Restores 10 hunger bars, gives 30 seconds of Super Speed and Regeneration!
    public static final FoodProperties ERIKS_PIZZA_FOOD = new FoodProperties.Builder()
        .nutrition(10)
        .saturationModifier(1.2f)
        .alwaysEdible() // Can eat even when full!
        .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 2), 1.0f) // Speed III for 30s
        .effect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 1), 1.0f)   // Regen II for 15s
        .build();

    // 2. Register the Item
    public static final Item ERIKS_PIZZA = registerItem(
        "eriks_pizza",
        new Item(new Item.Properties().food(ERIKS_PIZZA_FOOD).rarity(Rarity.EPIC))
    );
}
```

### 2. Add English Translation
In `src/main/resources/assets/erikcraft/lang/en_us.json`:
```json
"item.erikcraft.eriks_pizza": "Erik's Super Pizza"
```

### 3. Add Item Model
In `src/main/resources/assets/erikcraft/models/item/eriks_pizza.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "erikcraft:item/eriks_pizza"
  }
}
```

### 4. Add the 16x16 Texture
Put `eriks_pizza.png` in `src/main/resources/assets/erikcraft/textures/item/eriks_pizza.png`.

---

## Recipe 2: Adding an Item that Does Something on Right Click

Check out [EriksStarItem.java](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/EriksStarItem.java) for the complete working pattern:

```java
public class FireballWandItem extends Item {
    public FireballWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Summon an explosive fireball or launch the player into the sky!
            Vec3 look = player.getLookAngle();
            
            // Example: Rocket Jump (launch player forward)
            player.setDeltaMovement(look.x * 2.5, 1.2, look.z * 2.5);
            player.hurtMarked = true; // Tell Minecraft physics to apply velocity immediately

            player.getCooldowns().addCooldown(this, 20); // 1s cooldown
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
```

---

## Recipe 3: Adding a Crafting Recipe (No Code Required!)

Recipes in Minecraft 1.21 are purely JSON data!

Create a file at `src/main/resources/data/erikcraft/recipe/eriks_star.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    " D ",
    "DND",
    " D "
  ],
  "key": {
    "D": {
      "item": "minecraft:diamond"
    },
    "N": {
      "item": "minecraft:nether_star"
    }
  },
  "result": {
    "id": "erikcraft:eriks_star",
    "count": 1
  }
}
```

Now, in a crafting table, placing a Nether Star surrounded by 4 Diamonds crafts Erik's Star!
