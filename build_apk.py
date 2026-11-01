import os
import subprocess
import zipfile
import shutil

SDK_DIR = r"C:\Users\duo\android-sdk"
BUILD_TOOLS_DIR = os.path.join(SDK_DIR, "android-14")
PLATFORM_JAR = os.path.join(SDK_DIR, "android-35", "android.jar")
JDK_BIN = r"C:\Users\duo\AppData\Local\Programs\Microsoft\jdk-17.0.10.7-hotspot\bin"

AAPT2 = os.path.join(BUILD_TOOLS_DIR, "aapt2.exe")
ZIPALIGN = os.path.join(BUILD_TOOLS_DIR, "zipalign.exe")
D8_JAR = os.path.join(BUILD_TOOLS_DIR, "lib", "d8.jar")
APKSIGNER_JAR = os.path.join(BUILD_TOOLS_DIR, "lib", "apksigner.jar")

JAVAC = os.path.join(JDK_BIN, "javac.exe")
JAVA = os.path.join(JDK_BIN, "java.exe")
KEYTOOL = os.path.join(JDK_BIN, "keytool.exe")

PROJECT_DIR = r"C:\Users\duo\.gemini\antigravity\scratch\tetris-infinix"
BUILD_DIR = os.path.join(PROJECT_DIR, "build")

def run(cmd, desc):
    print(f"\n[+] {desc}...")
    res = subprocess.run(cmd, cwd=PROJECT_DIR, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"FAILED (code {res.returncode}):")
        print("STDOUT:", res.stdout)
        print("STDERR:", res.stderr)
        raise RuntimeError(f"Step failed: {desc}")
    print("Success.")

def main():
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
    os.makedirs(BUILD_DIR, exist_ok=True)
    
    gen_dir = os.path.join(BUILD_DIR, "gen")
    obj_dir = os.path.join(BUILD_DIR, "obj")
    dex_dir = os.path.join(BUILD_DIR, "dex")
    os.makedirs(gen_dir, exist_ok=True)
    os.makedirs(obj_dir, exist_ok=True)
    os.makedirs(dex_dir, exist_ok=True)

    compiled_res = os.path.join(BUILD_DIR, "compiled_res.zip")
    unaligned_apk = os.path.join(BUILD_DIR, "unaligned.apk")
    aligned_apk = os.path.join(BUILD_DIR, "aligned.apk")
    final_apk = os.path.join(PROJECT_DIR, "Tetrispro.apk")

    # Step 1: AAPT2 Compile resources
    run([AAPT2, "compile", "--dir", "res", "-o", compiled_res], "Compiling Android resources")

    # Step 2: AAPT2 Link resources and generate R.java
    run([
        AAPT2, "link",
        "-I", PLATFORM_JAR,
        "--manifest", "AndroidManifest.xml",
        "-o", unaligned_apk,
        "--java", gen_dir,
        compiled_res
    ], "Linking APK package resources")

    # Step 3: Find all Java source files
    java_files = []
    src_dir = os.path.join(PROJECT_DIR, "src")
    for root, _, files in os.walk(src_dir):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))
    for root, _, files in os.walk(gen_dir):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))

    print(f"Compiling {len(java_files)} Java files: {[os.path.basename(f) for f in java_files]}")
    run([
        JAVAC,
        "-cp", PLATFORM_JAR,
        "-source", "1.8",
        "-target", "1.8",
        "-d", obj_dir
    ] + java_files, "Compiling Pure Native Java sources")

    # Step 4: D8 convert bytecode to classes.dex
    class_files = []
    for root, _, files in os.walk(obj_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(root, f))

    run([
        JAVA, "-cp", D8_JAR, "com.android.tools.r8.D8",
        "--min-api", "24",
        "--output", dex_dir
    ] + class_files, "D8 desugaring & compiling to native classes.dex")

    # Step 5: Add classes.dex into unaligned.apk
    print("\n[+] Adding classes.dex into unaligned.apk...")
    dex_file = os.path.join(dex_dir, "classes.dex")
    with zipfile.ZipFile(unaligned_apk, "a") as z:
        z.write(dex_file, "classes.dex")
    print("Injected native classes.dex successfully.")

    # Step 6: Zipalign APK
    run([ZIPALIGN, "-f", "-p", "4", unaligned_apk, aligned_apk], "ZipAligning APK for 4-byte boundary")

    # Step 7: Create keystore if not present
    keystore = os.path.join(PROJECT_DIR, "debug.keystore")
    if not os.path.exists(keystore):
        run([
            KEYTOOL, "-genkeypair",
            "-keystore", keystore,
            "-storepass", "android",
            "-alias", "androiddebugkey",
            "-keypass", "android",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=TetrisPro, OU=Infinix, O=Gaming, L=Mobile, ST=Android, C=US"
        ], "Generating debug signing keystore")

    # Step 8: Apksigner (Scheme v1, v2, v3)
    run([
        JAVA, "-jar", APKSIGNER_JAR, "sign",
        "--ks", keystore,
        "--ks-pass", "pass:android",
        "--key-pass", "pass:android",
        "--ks-key-alias", "androiddebugkey",
        "--out", final_apk,
        aligned_apk
    ], "Signing APK with Scheme v1, v2, v3")

    # Verify signature
    run([
        JAVA, "-jar", APKSIGNER_JAR, "verify",
        "--verbose", final_apk
    ], "Verifying native APK signatures")

    # Also generate backward compatibility copy
    shutil.copyfile(final_apk, os.path.join(PROJECT_DIR, "TetrisPro-Infinix-Hot60Pro.apk"))

    print("\n" + "="*60)
    print("100% PURE NATIVE ANDROID APK BUILT AND SIGNED SUCCESSFULLY!")
    print(f"Output File: {final_apk}")
    print(f"File Size: {os.path.getsize(final_apk) / 1024:.1f} KB")
    print("="*60)

if __name__ == "__main__":
    main()
