# Model Reconstruction and AI Disclosure

## English

All model files used by this Minecraft 1.20.1 remaster of Ancient Warfare 2 were newly recreated with AI-assisted tools and workflows. They are not redistributed CodeChickenLib model files.

The models were recreated so the remaster could move away from the legacy CodeChickenLib model-rendering dependency. CodeChickenLib is not available on Fabric, and retaining that dependency would prevent or substantially obstruct a future Fabric version. The new model pipeline therefore also serves as preparation for possible future multi-loader support.

This disclosure applies to the recreated model geometry, model files, and animations used by the 1.20.1 remaster. It does not state that every asset in the mod is AI-created. Legacy Ancient Warfare textures, skins, structures, gameplay designs, names, and other contributed assets retain their original attribution and notices in `CREDITS.md`.

The use of newly recreated models does not imply endorsement of this unofficial remaster by the original Ancient Warfare authors or asset contributors.

The variable-sized gates and drawbridge are an exception to the recreated-geometry
description above: their original 1.12 `ModelGateBasic` / `ModelGateBridge` Java
geometry, UVs, and per-cell assembly rules have been converted to native 1.20
`ModelPart` rendering. Original GPL source attribution and texture credits apply.
No CodeChickenLib runtime or fixed-size preview model is required.

## 中文说明

本 Ancient Warfare 2 Minecraft 1.20.1 重制版中使用的模型文件，均通过 AI 辅助工具和工作流程重新制作，并非直接重新发布 CodeChickenLib 使用的旧模型文件。

重新制作模型的目的是摆脱旧版 CodeChickenLib 模型渲染依赖。Fabric 平台没有 CodeChickenLib，继续依赖它会阻止或严重妨碍未来的 Fabric 版本，因此新的模型流程也为将来可能进行的多加载器适配做准备。

本说明针对 1.20.1 重制版重新制作的模型几何、模型文件和动画，并不表示模组内所有素材均由 AI 制作。旧版 Ancient Warfare 的纹理、皮肤、结构、玩法设计、名称及其他贡献素材仍保留其原始署名，详见 `CREDITS.md`。

使用重新制作的模型不代表 Ancient Warfare 原作者或素材贡献者认可或背书此非官方重制版。

可变尺寸城门与吊桥是上述“重建几何”的例外：它们由 1.12 原版
`ModelGateBasic` / `ModelGateBridge` 的 Java 几何、UV 和逐格组合规则换算为
1.20 原生 `ModelPart` 渲染，保留原始 GPL 源码署名及贴图贡献说明，
不依赖 CodeChickenLib，也不直接加载固定尺寸的检查模型。
