# Repose - Mixins

This mod implements the same features that exist in delvr's [Repose](https://github.com/delvr/Repose):

* Walking up 1 block steps as if they are sloped (defaults to natural blocks such as dirt, grass, and stone)
* Granular blocks behave more granular-ly. E.g., a pillar of sand will fall and spread out in a mound


## Why write a mod that already exists?

I wanted to have these features on a SpongeForge server and there were incompatibilities in the way
methods were overwritten/remapped.

This re-write uses Mixins (which also means this is in Java) rather than raw class-transformers to
inject its changes into Minecraft and thus is more likely to be compatible as more mods transition to
using Mixins.
