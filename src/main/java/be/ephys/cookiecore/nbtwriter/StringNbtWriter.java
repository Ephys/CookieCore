package be.ephys.cookiecore.nbtwriter;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;

public class StringNbtWriter implements NbtWriter<String> {

  @Override
  public INBT toNbt(String data) {
    return StringNBT.func_229705_a_(data);
  }

  @Override
  public String fromNbt(INBT nbt) {
    return nbt.getString();
  }
}
