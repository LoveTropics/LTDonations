package com.lovetropics.donations.block;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DonationBlockModels {
    public static void generateFullPoweredBlock(DataGenContext<Block, ?> ctx, RegistrateBlockModelGenerator prov) {
        MultiVariant poweredVariant = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.create(
                ModelLocationUtils.getModelLocation(ctx.get(), "_on"),
                TextureMapping.cube(prov.blockTexture(ctx.get(), "_on")),
                prov.modelOutput
        ));
        MultiVariant unpoweredVariant = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.create(
                ModelLocationUtils.getModelLocation(ctx.get(), "_off"),
                TextureMapping.cube(prov.blockTexture(ctx.get(), "_off")),
                prov.modelOutput
        ));
        prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get()).with(PropertyDispatch.initial(BlockStateProperties.POWERED)
                .select(true, poweredVariant)
                .select(false, unpoweredVariant))
        );
    }
}
