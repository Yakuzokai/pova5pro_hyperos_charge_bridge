import os
import zipfile

MODULE_ID = "pova5pro_hyperos_charge_bridge"
ZIP_NAME = "LH8n_HyperOS_Charging_Bridge_Module_v1.3.1.zip"

def build():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    exclude = {".git", ".gitignore", ".gitattributes", "README.md", "build.py", ZIP_NAME}

    with zipfile.ZipFile(ZIP_NAME, "w", compression=zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk("."):
            dirs[:] = [d for d in dirs if d not in exclude and not d.startswith(".")]
            for f in files:
                if f in exclude or f.startswith(".") or f.endswith((".zip", ".idsig", ".log")):
                    continue
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, ".").replace("\\", "/")
                zi = zipfile.ZipInfo(rel_path)
                zi.compress_type = zipfile.ZIP_DEFLATED
                if (
                    rel_path.endswith(".sh")
                    or rel_path == "system/bin/bypass_charge"
                    or rel_path == "META-INF/com/google/android/update-binary"
                ):
                    zi.external_attr = 0o100755 << 16
                else:
                    zi.external_attr = 0o100644 << 16
                with open(full_path, "rb") as fp:
                    z.writestr(zi, fp.read())
                print(f"Added: {rel_path}")

    print(f"\nBuilt {ZIP_NAME} successfully!")

if __name__ == "__main__":
    build()
