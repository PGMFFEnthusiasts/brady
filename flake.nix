{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    systems.url = "systems";
  };

  outputs =
    {
      systems,
      nixpkgs,
      ...
    }:
    let
      eachSystem = f: nixpkgs.lib.genAttrs (import systems) (system: f nixpkgs.legacyPackages.${system});
    in
    {
      devShells = eachSystem (pkgs: {
        default = pkgs.mkShell {
          buildInputs = with pkgs; [
            zulu21
            gradle_8
          ];
          JAVA_HOME = pkgs.zulu21.home;
        };
      });
    };
}
