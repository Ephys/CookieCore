package nf.fr.ephys.cookiecore.helpers;

import com.google.common.collect.BiMap;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemReed;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.RegistryNamespaced;
import nf.fr.ephys.cookiecore.common.CookieCore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * https://github.com/CoFH/CoFHLib/blob/master/src/main/java/cofh/util/RegistryUtils.java
 */
public class RegistryHelper {
	@SuppressWarnings("unchecked")
	public static Object overwriteEntry(RegistryNamespaced registry, String name, Object newEntry) {
		try {
			Object oldEntry = registry.getObject(name);
			int id = registry.getIDForObject(oldEntry);

			BiMap map = (BiMap) registry.registryObjects;

			registry.underlyingIntegerMap.func_148746_a(newEntry, id);

			map.remove(name);
			map.forcePut(name, newEntry);

			BiMap reverse = (BiMap) registry.field_148758_b;
			reverse.remove(oldEntry);
			reverse.forcePut(newEntry, name);

			CookieCore.getLogger().info("RegistryUtils::overwriteEntry: overwriting '" + name + "' with " + newEntry.toString() + ": Operation success");

			return oldEntry;
		} catch (Exception e) {
			CookieCore.getLogger().error("RegistryUtils::overwriteEntry: overwriting '" + name + ": Operation failure (this is a bug !)");
			e.printStackTrace();

			return null;
		}
	}

	public static Block overwriteBlock(String name, Block newBlock) {
		Block oldBlock = (Block) overwriteEntry(Block.blockRegistry, name, newBlock);

		if (oldBlock == null) return null;

		if (name.startsWith("minecraft:")) {
			Field[] blocks = Blocks.class.getDeclaredFields();

			try {
				for (Field blockField : blocks) {
					if (blockField.get(null) == oldBlock) {
						blockField.setAccessible(true);

						Field modifiersField = Field.class.getDeclaredField("modifiers");
						modifiersField.setAccessible(true);
						modifiersField.setInt(blockField, blockField.getModifiers() & ~Modifier.FINAL);

						blockField.set(null, newBlock);

						CookieCore.getLogger().warn("RegistryUtils::overwriteBlock: overwrote net.minecraft.init.Blocks entry");

						break;
					}
				}
			} catch (Exception e) {
				CookieCore.getLogger().warn("RegistryUtils::overwriteBlock: failed to overwrite net.minecraft.init.Blocks entry");

				e.printStackTrace();
			}
		}

		overwriteItemBlock(name, newBlock);

		return oldBlock;
	}

	public static Item overwriteItem(String name, Item newItem) {
		Item oldItem = (Item) overwriteEntry(Item.itemRegistry, name, newItem);

		if (oldItem == null) return null;

		if (name.startsWith("minecraft:")) {
			Field[] items = Items.class.getDeclaredFields();

			try {
				for (Field itemField : items) {
					if (itemField.get(null) == oldItem) {
						itemField.setAccessible(true);

						Field modifiersField = Field.class.getDeclaredField("modifiers");
						modifiersField.setAccessible(true);
						modifiersField.setInt(itemField, itemField.getModifiers() & ~Modifier.FINAL);

						itemField.set(null, newItem);

						CookieCore.getLogger().warn("RegistryUtils::overwriteItem: overwrote net.minecraft.init.Items entry");

						break;
					}
				}
			} catch (Exception e) {
				CookieCore.getLogger().warn("RegistryUtils::overwriteItem: failed to overwrite net.minecraft.init.Items entry");

				e.printStackTrace();
			}
		}

		return oldItem;
	}

	public static boolean overwriteItemBlock(String name, Block newBlock) {
		Object oldItem = Item.itemRegistry.getObject(name);

		if (oldItem instanceof ItemBlock) {
			((ItemBlock) oldItem).field_150939_a = newBlock;

			return true;
		}

		return false;
	}

	public static void overwriteReedBlock(ItemReed reed, Block newBlock) {
		reed.field_150935_a = newBlock;
	}

	public static int removeItemRecipe(ItemStack stack) {
		List crafts = CraftingManager.getInstance().getRecipeList();

		int nbRemoved = 0;
		for (int i = 0; i < crafts.size(); i++) {
			if (crafts.get(i) instanceof IRecipe) {
				ItemStack output = ((IRecipe) crafts.get(i)).getRecipeOutput();

				if (output == null) continue;

				if (output.isItemEqual(stack)) {
					crafts.remove(i);
					nbRemoved++;
				}
			}
		}

		return nbRemoved;
	}

	/**
	 * Converts an itemstack name to an ItemStack instance
	 * The name must follow this format: modname:itemname@metadata
	 *                                   modname:itemname@[0-7],[9-14],15   will return itemstacks 0 to 7, 9 to 14 and 15
	 *                                   modname:itemname@*                 will return an itemstack for each metadata value
	 *                                   modname:itemname                   will default to metadata 0
	 *
	 * @param name  the item name
	 * @return      the matching itemstacks or null if an error occured
	 */
	public static ItemStack[] getItemStacks(String name) {
		String[] data = name.split("@");

		String itemname = data[0];

		Item item = (Item) Item.itemRegistry.getObject(itemname);

		if (item == null) return new ItemStack[0];
		
		if (data.length == 1)
			return new ItemStack[]{ new ItemStack(item) };
		
		return parseMetadata(item, data[1]);
	}
	
	private static ItemStack[] parseMetadata(Item item, String metadata) {
		if (metadata.length() == 1 && metadata.charAt(0) == '@') {
			ItemStack[] stacks = new ItemStack[item.getMaxDamage() + 1];
			
			for (int meta = 0; meta <= item.getMaxDamage(); meta++)
				stacks[meta] = new ItemStack(item, 1, meta);
			
			return stacks;
		}
		
		List<ItemStack> stacks = new ArrayList<>();
		String[] ranges = metadata.split(",");
		try {
			for (String range: ranges) {
				if (range.length() == 1) {
					int meta = Integer.parseInt(range);
					stacks.add(new ItemStack(item, 1, meta));
					
					continue;
				}
				
				String[] subRanges = range.substring(1, range.length() - 1).split("-");
				int lower = Integer.parseInt(subRanges[0]);
				int upper = Integer.parseInt(subRanges[1]);
				
				if (lower > upper) CookieCore.getLogger().error("Failed to parse metadata value " + metadata + ": invalid range " + range);
				for (int meta = lower; meta <= upper; meta++) {
					stacks.add(new ItemStack(item, 1, meta));
				}
			}
		} catch(NumberFormatException e) {
			CookieCore.getLogger().error("Failed to parse metadata value " + metadata, e);
			
			return null;
		}
		
		return stacks.toArray(new ItemStack[stacks.size()]);
	}
	
	/**
	 * @deprecated use getItemStacks(String name);
	 */
	@Deprecated
	public static ItemStack getItemStack(String name) {
		String[] data = name.split("@");

		String itemname = data[0];

		Item item = (Item) Item.itemRegistry.getObject(itemname);

		if (item == null) return null;

		try {
			int metadata;
			if (data.length > 1) {
				metadata = data[1].equals("*") ? 16 : Integer.parseInt(data[1]);
			} else {
				metadata = 0;
			}

			return new ItemStack(item, 1, metadata);
		} catch(NumberFormatException e) {
			return null;
		}
	}
}