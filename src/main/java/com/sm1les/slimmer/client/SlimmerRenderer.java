package com.sm1les.slimmer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sm1les.slimmer.SlimmerMod;
import com.sm1les.slimmer.entity.Slimmer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Рендер Слиммера: модель из Slimmer.bbmodel рисуется напрямую (с UV из Blockbench),
 * анимации считаются кодом: idle, ходьба, бег, подкрадывание, прыжок, висение на спине, раздувание.
 *
 * Индексы частей: 0 leg1, 1 leg2, 2 leg3, 3 leg4, 4 body, 5 head.
 * Перед модели = +X в Blockbench. leg1/leg2 - передние, leg3/leg4 - задние.
 * Поворот rot[i][2] (вокруг Z) = качание вперёд/назад, rot[i][0] (вокруг X) = разведение в стороны.
 */
public class SlimmerRenderer extends EntityRenderer<Slimmer> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SlimmerMod.MODID, "textures/entity/slimmer.png");

    private static final float DEG = Mth.DEG_TO_RAD;

    public SlimmerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55F;
    }

    @Override
    public ResourceLocation getTextureLocation(Slimmer entity) {
        return TEXTURE;
    }

    @Override
    public void render(Slimmer e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        int state = e.getState();
        float age = e.tickCount + pt;
        float limbSwing = e.walkAnimation.position(pt);
        float limbSpeed = Math.min(1.0F, e.walkAnimation.speed(pt));

        float crouch = Mth.lerp(pt, e.crouchO, e.crouch);
        float run = Mth.lerp(pt, e.runO, e.run);
        float leap = Mth.lerp(pt, e.leapO, e.leap);
        float cling = Mth.lerp(pt, e.clingO, e.cling);
        float swell = e.getSwell(pt);

        ps.pushPose();

        // ---- позиция и поворот ----
        float bodyYaw;
        Player host = e.isOnBack() ? e.getHost() : null;
        if (host != null) {
            bodyYaw = Mth.rotLerp(pt, host.yBodyRotO, host.yBodyRot);
            if (state == Slimmer.CLING) {
                // прилипает к спине игрока без задержки (по кадрам)
                Vec3 target = Slimmer.backPoint(host.getPosition(pt), bodyYaw);
                Vec3 self = e.getPosition(pt);
                ps.translate(target.x - self.x, target.y - self.y, target.z - self.z);
            }
        } else {
            bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        }

        float netHead = 0.0F;
        float pitch = 0.0F;
        if (host == null) {
            float headYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
            netHead = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -70.0F, 70.0F);
            pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        }

        // модель смотрит в +X, мир - в +Z: поворачиваем на -90 градусов
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw - 90.0F));

        // падение при смерти
        if (e.deathTime > 0) {
            float f = Mth.sqrt(((float) e.deathTime + pt - 1.0F) / 20.0F * 1.6F);
            ps.mulPose(Axis.XP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
        }

        ps.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        // раздувание перед взрывом (как у крипера, но сильнее)
        float f = swell;
        float f2 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = f * f;
        f = f * f;
        float sh = (1.0F + f * 0.5F) * f2;
        float sv = (1.0F + f * 0.15F) / f2;
        ps.scale(sh, sv, sh);

        // наклон всей модели: вперёд (к игроку) когда висит, чуть вверх в прыжке
        float modelPitch = -0.2F * cling + 0.35F * leap;
        ps.translate(0.0F, 12.0F, 0.0F);
        ps.mulPose(Axis.ZP.rotation(modelPitch));
        ps.translate(0.0F, -12.0F, 0.0F);

        ps.translate(-SlimmerGeometry.CENTER_X, 0.0F, -SlimmerGeometry.CENTER_Z);

        // ---- анимация ----
        float[][] rot = new float[6][3];
        float[][] tr = new float[6][3];
        float[] sc = {1, 1, 1, 1, 1, 1};

        // ходьба / бег: диагональные пары ног (leg1 + leg3 против leg2 + leg4)
        float freq = Mth.lerp(run, 0.6662F, 0.95F);
        freq = Mth.lerp(crouch, freq, 0.55F);
        float amp = Mth.lerp(run, 1.4F, 1.15F) * (1.0F - 0.45F * crouch);
        float a = Mth.cos(limbSwing * freq) * amp * limbSpeed;
        rot[0][2] = a;
        rot[2][2] = a;
        rot[1][2] = -a;
        rot[3][2] = -a;

        // подпрыгивание при ходьбе
        float bob = Math.abs(Mth.cos(limbSwing * freq)) * (0.8F + 0.6F * run) * limbSpeed;
        tr[4][1] += bob;
        tr[5][1] += bob;

        // idle: дыхание и покачивание головой
        float idle = 1.0F - Math.min(1.0F, limbSpeed * 4.0F);
        float breathe = Mth.sin(age * 0.09F) * 0.25F * idle;
        tr[4][1] += breathe;
        tr[5][1] += breathe;
        rot[5][2] += Mth.sin(age * 0.07F) * 0.04F * idle;
        rot[5][1] += Mth.sin(age * 0.05F) * 0.06F * idle;
        rot[0][2] += Mth.sin(age * 0.06F) * 0.03F * idle;
        rot[1][2] -= Mth.sin(age * 0.06F) * 0.03F * idle;

        // голова следит за целью
        rot[5][1] += -netHead * DEG;
        rot[5][2] += -pitch * DEG;

        // подкрадывание: присел, ноги короче, наклон вперёд
        for (int i = 0; i < 4; i++) {
            tr[i][1] -= 3.0F * crouch;
            sc[i] = 1.0F - 0.3F * crouch;
        }
        tr[4][1] -= 3.0F * crouch;
        tr[5][1] -= 3.0F * crouch;
        rot[4][2] -= 0.3F * crouch;
        rot[5][2] += 0.25F * crouch;

        // бег: наклон вперёд
        rot[4][2] -= 0.22F * run;
        rot[5][2] += 0.18F * run;

        // прыжок на спину: ноги выброшены вперёд
        for (int i = 0; i < 4; i++) {
            rot[i][2] = Mth.lerp(leap, rot[i][2], 1.0F);
        }
        rot[4][2] = Mth.lerp(leap, rot[4][2], -0.15F);
        rot[5][2] = Mth.lerp(leap, rot[5][2], 0.35F);

        // висит на спине: ноги обхватывают игрока, мелкая дрожь
        float wob = Mth.sin(age * 0.7F) * 0.06F;
        for (int i = 0; i < 4; i++) {
            float sign = (i % 2 == 0) ? 1.0F : -1.0F;
            rot[i][2] = Mth.lerp(cling, rot[i][2], 1.15F + wob * sign);
        }
        rot[0][0] += 0.5F * cling;  // leg1 (лево-перед)
        rot[3][0] += 0.5F * cling;  // leg4 (лево-зад)
        rot[1][0] -= 0.5F * cling;  // leg2 (право-перед)
        rot[2][0] -= 0.5F * cling;  // leg3 (право-зад)
        rot[5][2] += 0.1F * cling;

        // дрожь тела в конце фитиля
        if (swell > 0.0F) {
            tr[4][0] += Mth.sin(age * 2.7F) * 0.35F * swell;
            tr[4][2] += Mth.cos(age * 3.1F) * 0.35F * swell;
        }

        // ---- рендер ----
        float white = ((int) (swell * 10.0F)) % 2 == 0 ? 0.0F : Mth.clamp(swell, 0.5F, 1.0F);
        boolean hurt = e.hurtTime > 0 || e.deathTime > 0;
        int overlay = OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(hurt));
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            applyPart(ps, i, rot, tr, sc);
            drawCube(ps.last(), vc, i, light, overlay);
            ps.popPose();
        }

        // голова - дочерняя часть тела (двигается вместе с ним)
        ps.pushPose();
        applyPart(ps, 4, rot, tr, sc);
        drawCube(ps.last(), vc, 4, light, overlay);
        applyPart(ps, 5, rot, tr, sc);
        drawCube(ps.last(), vc, 5, light, overlay);
        ps.popPose();

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buf, light);
    }

    private static void applyPart(PoseStack ps, int i, float[][] rot, float[][] tr, float[] sc) {
        float[] p = SlimmerGeometry.PIVOT[i];
        ps.translate(p[0] + tr[i][0], p[1] + tr[i][1], p[2] + tr[i][2]);
        ps.mulPose(new Quaternionf().rotationZYX(rot[i][2], rot[i][1], rot[i][0]));
        ps.scale(1.0F, sc[i], 1.0F);
        ps.translate(-p[0], -p[1], -p[2]);
    }

    private static void drawCube(PoseStack.Pose pose, VertexConsumer vc, int i, int light, int overlay) {
        float[] b = SlimmerGeometry.BOX[i];
        float[][] uv = SlimmerGeometry.UV[i];
        float x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];

        // north (-Z)
        quad(pose, vc, uv[0], x2, y2, z1, x1, y2, z1, x1, y1, z1, x2, y1, z1, 0, 0, -1, light, overlay);
        // south (+Z)
        quad(pose, vc, uv[1], x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2, 0, 0, 1, light, overlay);
        // east (+X) - лицо
        quad(pose, vc, uv[2], x2, y2, z2, x2, y2, z1, x2, y1, z1, x2, y1, z2, 1, 0, 0, light, overlay);
        // west (-X)
        quad(pose, vc, uv[3], x1, y2, z1, x1, y2, z2, x1, y1, z2, x1, y1, z1, -1, 0, 0, light, overlay);
        // up
        quad(pose, vc, uv[4], x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, 0, 1, 0, light, overlay);
        // down
        quad(pose, vc, uv[5], x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, 0, -1, 0, light, overlay);
    }

    /** Углы: левый-верхний, правый-верхний, правый-нижний, левый-нижний (как в UV Blockbench). */
    private static void quad(PoseStack.Pose pose, VertexConsumer vc, float[] uv,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float nx, float ny, float nz, int light, int overlay) {
        float u1 = uv[0] / 64.0F, v1 = uv[1] / 64.0F, u2 = uv[2] / 64.0F, v2 = uv[3] / 64.0F;
        vertex(pose, vc, ax, ay, az, u1, v1, nx, ny, nz, light, overlay);
        vertex(pose, vc, bx, by, bz, u2, v1, nx, ny, nz, light, overlay);
        vertex(pose, vc, cx, cy, cz, u2, v2, nx, ny, nz, light, overlay);
        vertex(pose, vc, dx, dy, dz, u1, v2, nx, ny, nz, light, overlay);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int light, int overlay) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
