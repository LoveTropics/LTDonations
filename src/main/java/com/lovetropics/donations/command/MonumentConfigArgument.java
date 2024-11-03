package com.lovetropics.donations.command;

import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.monument.MonumentData;
import com.lovetropics.donations.monument.MonumentType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class MonumentConfigArgument implements ArgumentType<MonumentData> {
	public static final DynamicCommandExceptionType ERROR_UNKNOWN_TYPE = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_TYPE_UNKNOWN::format);
	public static final DynamicCommandExceptionType ERROR_INVALID_CONFIG = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_CONFIG_INVALID::format);

	private final HolderLookup.Provider registries;

	public MonumentConfigArgument(CommandBuildContext buildContext) {
		registries = buildContext;
	}

	public static MonumentConfigArgument monumentConfig(CommandBuildContext buildContext) {
		return new MonumentConfigArgument(buildContext);
	}

	public static MonumentData getMonumentConfig(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, MonumentData.class);
	}

	@Override
	public MonumentData parse(StringReader reader) throws CommandSyntaxException {
		return readMonument(reader, registries);
	}

	private static MonumentData readMonument(StringReader reader, HolderLookup.Provider registries) throws CommandSyntaxException {
		MonumentType type = readMonumentType(reader);
		return readMonument(reader, type, registries);
	}

	private static MonumentType readMonumentType(StringReader reader) throws CommandSyntaxException {
		String id = reader.readString();
		MonumentType type = MonumentType.CODEC.byName(id);
		if (type == null) {
			throw ERROR_UNKNOWN_TYPE.createWithContext(reader, id);
		}
		return type;
	}

	private static MonumentData readMonument(StringReader reader, MonumentType type, HolderLookup.Provider registries) throws CommandSyntaxException {
		CompoundTag tag;
		if (reader.canRead() && reader.peek() == '{') {
			tag = new TagParser(reader).readStruct();
		} else {
			tag = new CompoundTag();
		}
		return type.codec().codec().parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
				.getOrThrow(ERROR_INVALID_CONFIG::create);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Arrays.stream(MonumentType.values()).map(MonumentType::getSerializedName), builder);
	}
}
