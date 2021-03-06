package be.ephys.cookiecore.syncable;

import be.ephys.cookiecore.nbtwriter.NbtWriter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class field as to be synced from server to client.
 *
 * It will then be handled by
 * PersisterRegistry.getPersisterFor(obj).writeToNbt()
 * PersisterRegistry.getPersisterFor(obj).readFromNbt()
 */
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sync {

  String name() default "";

  Class<? extends NbtWriter> serializer() default NbtWriter.class;
}
