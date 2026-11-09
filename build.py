import os
import zipfile

MODULE_ID = "pova5pro_hyperos_charge_bridge"
ZIP_NAME = "LH8n_HyperOS_Charging_Bridge_Module_v1.0.0.zip"

def build():
    # Files to exclude from zip
    exclude = {".git", ".gitignore", "build.py", ZIP_NAME}
    
    with zipfile.ZipFile(ZIP_NAME, "w", compression=zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk("."):
            dirs[:] = [d for d in dirs if d not in exclude and not d.startswith(".")]
            for f in files:
                if f in exclude or f.endswith(".zip"):
                    continue
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, ".").replace("\\", "/")
                z.write(full_path, rel_path)
                print(f"Added: {rel_path}")

    print(f"\nBuilt {ZIP_NAME} successfully!")

if __name__ == "__main__":
    build()
