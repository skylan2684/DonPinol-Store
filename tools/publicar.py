#!/usr/bin/env python3
"""
Publica en la tienda una versión de una app cuyo código está en un repositorio PRIVADO.

  python3 tools/publicar.py <repo-privado> <tag> <slug> [--cambios "texto de novedades"]

Ejemplo:
  python3 tools/publicar.py skylan2684/LecoLite v1.0.9 lecolite --cambios "• Mapa con recorrido GPS"

Qué hace:
 1. Baja la APK del Release <tag> del repo privado (necesita `gh` con acceso a ese repo).
 2. La guarda en apks/ de este repo público (quita la versión anterior de esa app).
 3. Actualiza catalog.json (versión, enlace, SHA-256, tamaño, fecha y novedades) de la app con ese ícono/slug.
Luego hay que hacer commit y push de catalog.json y apks/.
El versionCode sale del último número del tag (v1.0.9 → 9), igual que en el CI de las apps.
"""
import argparse, datetime, hashlib, json, os, subprocess, sys, tempfile

STORE = "skylan2684/DonPinol-Store"
MESES = "ene feb mar abr may jun jul ago sep oct nov dic".split()


def gh(*args, **kw):
    return subprocess.run(["gh", *args], check=True, capture_output=True, **kw).stdout


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo"); ap.add_argument("tag"); ap.add_argument("slug")
    ap.add_argument("--cambios", default=None)
    a = ap.parse_args()

    assets = json.loads(gh("api", f"repos/{a.repo}/releases/tags/{a.tag}"))["assets"]
    apk = next(x for x in assets if x["name"].endswith(".apk"))
    tmp = tempfile.mkdtemp()
    path = os.path.join(tmp, apk["name"])
    with open(path, "wb") as f:
        f.write(gh("api", "-H", "Accept: application/octet-stream", f"repos/{a.repo}/releases/assets/{apk['id']}"))
    data = open(path, "rb").read()
    sha = hashlib.sha256(data).hexdigest()

    # La APK se guarda en apks/ de este repositorio público (la versión anterior de la misma app se quita).
    apks = os.path.join(os.path.dirname(__file__), "..", "apks")
    os.makedirs(apks, exist_ok=True)
    prefix = apk["name"].rsplit("-", 1)[0] + "-"
    for old in os.listdir(apks):
        if old.startswith(prefix): os.remove(os.path.join(apks, old))
    open(os.path.join(apks, apk["name"]), "wb").write(data)

    version_name = a.tag.lstrip("v")
    version_code = int(version_name.split(".")[-1])
    cat_path = os.path.join(os.path.dirname(__file__), "..", "catalog.json")
    cat = json.load(open(cat_path, encoding="utf-8"))
    app = next(x for x in cat["apps"] if x.get("icon", "").endswith(f"/{a.slug}.png") or x["id"].endswith(a.slug))
    hoy = datetime.date.today()
    app.update(
        versionCode=version_code, versionName=version_name, sha256=sha, size=len(data),
        apkUrl=f"https://raw.githubusercontent.com/{STORE}/main/apks/{apk['name']}",
        updated=f"{hoy.day} {MESES[hoy.month - 1]} {hoy.year}",
    )
    if a.cambios: app["changelog"] = a.cambios
    cat["updatedAt"] = hoy.isoformat()
    json.dump(cat, open(cat_path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    open(cat_path, "a").write("\n")
    print(f"{app['name']} {version_name} publicada. Ahora: git commit catalog.json && git push")


if __name__ == "__main__":
    sys.exit(main())
