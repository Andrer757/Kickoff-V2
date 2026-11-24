class Kickoff < Formula
  desc "Deterministic Android Scaffolding Engine & Day-2 Architecture Platform"
  homepage "https://github.com/Andrer757/Kickoff-V2"
  url "https://github.com/Andrer757/Kickoff-V2/releases/download/v1.1.0/kickoff-cli-1.1.0.tar"
  sha256 "832947ab662c2ae788509e4c13a0b70b70ffe1518ad36ce6a9fe3f780696ae3d"
  license "MIT"

  depends_on "openjdk@21"

  def install
    rm_f Dir["bin/*.bat"]
    libexec.install Dir["*"]
    env = Language::Java.overridable_java_home_env("21")
    (bin/"kickoff").write_env_script libexec/"bin/kickoff-cli", env
    (bin/"kickoff-cli").write_env_script libexec/"bin/kickoff-cli", env
  end

  test do
    output = shell_output("#{bin}/kickoff --help")
    assert_match "Usage: kickoff", output
  end
end
