package com.bikerboys.scscompat.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.sarocesch.saroscharsystem.commands.CharCommand;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;

import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Mixin(value = CharCommand.class, remap = false)
public abstract class TestMixin {


    @Shadow
    protected static JsonArray savePlayerEffects(ServerPlayer player) {
        return null;
    }

    @Shadow
    protected static void loadPlayerEffects(ServerPlayer player, JsonArray effectsArray) {
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private static JsonObject saveCurrentCharacterData(ServerPlayer player, int charId) {
        JsonObject characterData = new JsonObject();
        characterData.addProperty("id", charId);
        characterData.addProperty("x", player.getX());
        characterData.addProperty("y", player.getY());
        characterData.addProperty("z", player.getZ());
        PrintStream var10000 = System.out;
        double var10001 = player.getX();
        var10000.println("Saving position: X=" + var10001 + " Y=" + player.getY() + " Z=" + player.getZ());
        characterData.addProperty("health", player.getHealth());
        characterData.addProperty("hunger", player.getFoodData().getFoodLevel());
        characterData.addProperty("experience", player.experienceLevel);
        characterData.add("effects", savePlayerEffects(player));
        characterData.add("inventory", savePlayerInventory(player));
        characterData.add("origin", saveOrigins(player));
        return characterData;
    }



    private static void loadOrigins(ServerPlayer player, JsonArray originsArray) {
        LazyOptional<IOriginContainer> iorigins = IOriginContainer.get(player);
        if (iorigins.isPresent()) {
            IOriginContainer originContainer = iorigins.resolve().get();

            originsArray.forEach(element -> {
                JsonObject originData = element.getAsJsonObject();

                // Parse layer key
                ResourceLocation layerId = ResourceLocation.parse(originData.get("layer").getAsString());
                ResourceKey<OriginLayer> layerKey =
                        ResourceKey.create(OriginsDynamicRegistries.LAYERS_REGISTRY, layerId);

                // Parse origin key
                ResourceLocation originId = ResourceLocation.parse(originData.get("origin").getAsString());
                ResourceKey<Origin> originKey =
                        ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, originId);

                // Assign
                originContainer.setOrigin(layerKey, originKey);
            });

            // Push to client
            originContainer.synchronize();
        }
    }


    /**
     * @author
     * @reason
     */
    @Overwrite
    private static void loadCharacterData(ServerPlayer player, JsonObject characterData) {
        if (characterData.has("health")) {
            player.setHealth(characterData.get("health").getAsFloat());
            System.out.println("Health set to: " + characterData.get("health").getAsFloat());
        }

        if (characterData.has("hunger")) {
            player.getFoodData().setFoodLevel(characterData.get("hunger").getAsInt());
            System.out.println("Hunger set to: " + characterData.get("hunger").getAsInt());
        }

        if (characterData.has("experience")) {
            player.setExperienceLevels(characterData.get("experience").getAsInt());
            System.out.println("Experience set to: " + characterData.get("experience").getAsInt());
        }

        if (characterData.has("effects")) {
            loadPlayerEffects(player, characterData.getAsJsonArray("effects"));
            System.out.println("Effects loaded.");
        }

        if (characterData.has("inventory")) {
            loadPlayerInventory(player, characterData.getAsJsonArray("inventory"));
            System.out.println("Inventory loaded.");
        }

        if (characterData.has("origin")) {
            loadOrigins(player, characterData.getAsJsonArray("origin"));
            System.out.println("origin loaded");
        }


    }




    private static JsonArray saveOrigins(ServerPlayer player) {
        JsonArray originsArray = new JsonArray();

        LazyOptional<IOriginContainer> iorigins = IOriginContainer.get(player);
        if (iorigins.isPresent()) {
            IOriginContainer originContainer = iorigins.resolve().get();

            originContainer.getOrigins().forEach((layerKey, originKey) -> { // originKey is ResourceKey<Origin>
                JsonObject originData = new JsonObject();

                originData.addProperty("layer", layerKey.location().toString());
                originData.addProperty("origin", originKey.location().toString());

                originsArray.add(originData);
            });
        }

        return originsArray;
    }





    /**
     * @author
     * @reason
     */
    @Overwrite
    private static JsonArray savePlayerInventory(ServerPlayer player) {
        JsonArray inventoryArray = new JsonArray();

        int i;
        ItemStack itemStack;
        JsonObject itemData;
        for(i = 0; i < player.getInventory().items.size(); ++i) {
            itemStack = (ItemStack)player.getInventory().items.get(i);
            if (!itemStack.isEmpty()) {
                itemData = new JsonObject();
                itemData.addProperty("slot", i);
                itemData.addProperty("type", "main");
                itemData.addProperty("item", ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString());
                itemData.addProperty("count", itemStack.getCount());
                if (itemStack.hasTag()) {
                    itemData.addProperty("nbt", itemStack.getTag().toString());
                }

                inventoryArray.add(itemData);
            }
        }

        for(i = 0; i < player.getInventory().armor.size(); ++i) {
            itemStack = (ItemStack)player.getInventory().armor.get(i);
            if (!itemStack.isEmpty()) {
                itemData = new JsonObject();
                itemData.addProperty("slot", i);
                itemData.addProperty("type", "armor");
                itemData.addProperty("item", ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString());
                itemData.addProperty("count", itemStack.getCount());
                if (itemStack.hasTag()) {
                    itemData.addProperty("nbt", itemStack.getTag().toString());
                }

                inventoryArray.add(itemData);
            }
        }

        ItemStack offhandItem = (ItemStack)player.getInventory().offhand.get(0);
        if (!offhandItem.isEmpty()) {
            itemData = new JsonObject();
            itemData.addProperty("slot", 0);
            itemData.addProperty("type", "offhand");
            itemData.addProperty("item", ForgeRegistries.ITEMS.getKey(offhandItem.getItem()).toString());
            itemData.addProperty("count", offhandItem.getCount());
            if (offhandItem.hasTag()) {
                itemData.addProperty("nbt", offhandItem.getTag().toString());
            }

            inventoryArray.add(itemData);
        }


        // --- Curios ---
        Optional<ICuriosItemHandler> curiosItemHandler = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosItemHandler.isPresent()) {
            ICuriosItemHandler handler = curiosItemHandler.get();

            handler.getCurios().forEach((curioType, stacksHandler) -> {
                IItemHandlerModifiable stacks = stacksHandler.getStacks();

                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    ItemStack curioStack = stacks.getStackInSlot(slot);
                    if (!curioStack.isEmpty()) {
                        JsonObject curioData = new JsonObject();
                        curioData.addProperty("slot", slot);
                        curioData.addProperty("type", "curios");
                        curioData.addProperty("curioType", curioType); // important!
                        curioData.addProperty("item", ForgeRegistries.ITEMS.getKey(curioStack.getItem()).toString());
                        curioData.addProperty("count", curioStack.getCount());
                        if (curioStack.hasTag()) {
                            curioData.addProperty("nbt", curioStack.getTag().toString());
                        }
                        inventoryArray.add(curioData);
                    }
                }
            });
        }


        // --- cosmetic armor (CosArmorAPI) ---
        CAStacksBase caStacks = CosArmorAPI.getCAStacks(player.getUUID());
        for (i = 0; i < 4; i++) { // slots 0–3 = head, chest, legs, feet
            ItemStack cosStack = caStacks.getStackInSlot(i);
            if (!cosStack.isEmpty()) {
                JsonObject cosData = new JsonObject();
                cosData.addProperty("slot", i);
                cosData.addProperty("type", "cosapi");
                cosData.addProperty("item", ForgeRegistries.ITEMS.getKey(cosStack.getItem()).toString());
                cosData.addProperty("count", cosStack.getCount());
                if (cosStack.hasTag()) {
                    cosData.addProperty("nbt", cosStack.getTag().toString());
                }
                inventoryArray.add(cosData);
            }
        }

        return inventoryArray;
    }


    /**
     * @author
     * @reason
     */
    @Overwrite
    private static void loadPlayerInventory(ServerPlayer player, JsonArray inventoryArray) {
        player.getInventory().clearContent();

        CAStacksBase caStacks = CosArmorAPI.getCAStacks(player.getUUID());


        for (int i = 0; i < 4; i++) {
            caStacks.setStackInSlot(i, ItemStack.EMPTY);
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((curioType, stacksHandler) -> {
                IItemHandlerModifiable stacks = stacksHandler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    stacks.setStackInSlot(slot, ItemStack.EMPTY);
                }
            });
        });

        inventoryArray.forEach((element) -> {
            JsonObject itemData = element.getAsJsonObject();
            String itemName = itemData.get("item").getAsString();
            int count = itemData.get("count").getAsInt();
            int slot = itemData.get("slot").getAsInt();
            String type = itemData.has("type") ? itemData.get("type").getAsString() : "main";
            ResourceLocation itemLocation = ResourceLocation.parse(itemName);
            Item item = (Item) ForgeRegistries.ITEMS.getValue(itemLocation);
            if (item != null) {
                ItemStack stack = new ItemStack(item, count);
                if (itemData.has("nbt")) {
                    try {
                        stack.setTag(TagParser.parseTag(itemData.get("nbt").getAsString()));
                    } catch (Exception var12) {
                        Exception e = var12;
                        e.printStackTrace();
                    }
                }

                switch (type) {
                    case "armor":
                        player.getInventory().armor.set(slot, stack);
                        break;

                    case "curios":
                        Optional<ICuriosItemHandler> curiosItemHandler = CuriosApi.getCuriosInventory(player).resolve();
                        if (curiosItemHandler.isPresent()) {
                            ICuriosItemHandler handler = curiosItemHandler.get();

                            // Your JSON should include which curio slot group to put this item in
                            // For example: "curioType": "ring"
                            String curioType = itemData.has("curioType") ? itemData.get("curioType").getAsString() : "curio";

                            handler.getStacksHandler(curioType).ifPresent(stacksHandler -> {
                                IItemHandlerModifiable stacks = stacksHandler.getStacks();
                                if (slot >= 0 && slot < stacks.getSlots()) {
                                    stacks.setStackInSlot(slot, stack);
                                }
                            });
                        }
                        break;

                    case "cosapi":

                        if (slot >= 0 && slot < 4) {
                            caStacks.setStackInSlot(slot, stack);
                        }
                        break;


                    case "offhand":
                        player.getInventory().offhand.set(slot, stack);
                        break;
                    case "main":
                    default:
                        player.getInventory().items.set(slot, stack);
                }
            }

        });
    }





}
