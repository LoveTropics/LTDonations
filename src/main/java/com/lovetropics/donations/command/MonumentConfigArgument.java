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
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class MonumentConfigArgument implements ArgumentType<MonumentData> {
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_TYPE = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_TYPE_UNKNOWN::format);
    public static final DynamicCommandExceptionType ERROR_INVALID_CONFIG = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_CONFIG_INVALID::format);

    private final TagParser<?> tagParser;

    public MonumentConfigArgument(CommandBuildContext buildContext) {
        tagParser = TagParser.create(buildContext.createSerializationContext(NbtOps.INSTANCE));
    }

    public static MonumentConfigArgument monumentConfig(CommandBuildContext buildContext) {
        return new MonumentConfigArgument(buildContext);
    }

    public static MonumentData getMonumentConfig(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, MonumentData.class);
    }

    @Override
    public MonumentData parse(StringReader reader) throws CommandSyntaxException {
        return readMonument(reader);
    }

    private MonumentData readMonument(StringReader reader) throws CommandSyntaxException {
        MonumentType type = readMonumentType(reader);
        return readMonument(reader, type, tagParser);
    }

    private MonumentType readMonumentType(StringReader reader) throws CommandSyntaxException {
        String id = reader.readString();
        MonumentType type = MonumentType.CODEC.byName(id);
        if (type == null) {
            throw ERROR_UNKNOWN_TYPE.createWithContext(reader, id);
        }
        return type;
    }

    private static <T> MonumentData readMonument(StringReader reader, MonumentType type, TagParser<T> parser) throws CommandSyntaxException {
        T tag;
        if (reader.canRead() && reader.peek() == '{') {
            tag = parser.parseAsArgument(reader);
        } else {
            tag = parser.getOps().emptyMap();
        }
        return type.codec().codec().parse(parser.getOps(), tag)
                .getOrThrow(ERROR_INVALID_CONFIG::create);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(MonumentType.values()).map(MonumentType::getSerializedName), builder);
    }
}
