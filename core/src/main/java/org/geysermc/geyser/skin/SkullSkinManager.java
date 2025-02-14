/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.skin;

import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerSkinPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SkullSkinManager extends SkinManager {

    public static SerializedSkin buildSkullEntryManually(String skinId, byte[] skinData) {
        // Prevents https://cdn.discordapp.com/attachments/613194828359925800/779458146191147008/unknown.png
        skinId = skinId + "_skull";
        return SerializedSkin.of(
                skinId, "", SkinProvider.SKULL_GEOMETRY.geometryName(), ImageData.of(skinData), Collections.emptyList(),
                ImageData.of(SkinProvider.EMPTY_CAPE.capeData()), SkinProvider.SKULL_GEOMETRY.geometryData(),
                "", true, false, false, SkinProvider.EMPTY_CAPE.capeId(), skinId
        );
    }

    public static void requestAndHandleSkin(SkullPlayerEntity entity, GeyserSession session,
                                            Consumer<SkinProvider.Skin> skinConsumer) {
        BiConsumer<SkinProvider.Skin, Throwable> applySkin = (skin, throwable) -> {
            try {
                PlayerSkinPacket packet = new PlayerSkinPacket();
                packet.setUuid(entity.getUuid());
                packet.setOldSkinName("");
                packet.setNewSkinName(skin.getTextureUrl());
                packet.setSkin(buildSkullEntryManually(skin.getTextureUrl(), skin.getSkinData()));
                packet.setTrustedSkin(true);
                session.sendUpstreamPacket(packet);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.skin.fail", entity.getUuid()), e);
            }

            if (skinConsumer != null) {
                skinConsumer.accept(skin);
            }
        };

        GameProfileData data = GameProfileData.from(entity);
        if (data == null) {
            GeyserImpl.getInstance().getLogger().debug("Using fallback skin for skull at " + entity.getSkullPosition() +
                    " with texture value: " + entity.getTexturesProperty() + " and UUID: " + entity.getSkullUUID());
            // No texture available, fallback using the UUID
            SkinProvider.SkinData fallback = SkinProvider.determineFallbackSkinData(entity.getSkullUUID());
            applySkin.accept(fallback.skin(), null);
        } else {
            SkinProvider.requestSkin(entity.getUuid(), data.skinUrl(), true)
                    .whenCompleteAsync(applySkin);
        }
    }

}
