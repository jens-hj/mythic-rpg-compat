{
  description = "Mythic RPG Compat - Minecraft Fabric mod dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk = pkgs.jdk25;
        # Native libs needed by `./gradlew runClient` (LWJGL / GLFW / OpenAL)
        runtimeLibs = with pkgs; [
          libGL
          glfw
          openal
          flite
          libpulseaudio
          udev
          wayland
          libxkbcommon
          libx11
          libxcursor
          libxrandr
          libxxf86vm
          libxext
        ];
      in {
        devShells.default = pkgs.mkShell {
          packages = [ jdk (pkgs.python3.withPackages (ps: [ ps.pillow ])) pkgs.git ];
          JAVA_HOME = "${jdk}";
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath runtimeLibs;
        };
      });
}
