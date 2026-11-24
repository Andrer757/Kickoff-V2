class Kickoff < Formula
  desc "Deterministic Android Scaffolding Engine & Day-2 Architecture Platform"
  homepage "https://github.com/Andrer757/Kickoff-V2"
  url "https://github.com/Andrer757/Kickoff-V2/releases/download/v1.0.0/kickoff-cli-1.0.0.tar"
  sha256 "c08f4f92e850597d91add1ba1fca2d5b79d48bbb6332a098b09730fdc627efad"
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
