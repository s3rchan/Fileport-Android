"""Generate Android launcher icons from the approved Newdesign/logo.png."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops

root=Path(__file__).resolve().parents[1]
asset=root/"app/src/main/assets/branding/logo.png"
if not asset.is_file():
 raise FileNotFoundError("Run scripts/install_newdesign_assets.py first")
art=Image.open(asset).convert("RGBA")
art.thumbnail((490,490),Image.Resampling.LANCZOS)
image=Image.new("RGBA",(512,512),(0,0,0,0))
image.alpha_composite(art,((512-art.width)//2,(512-art.height)//2))
res=root/"app/src/main/res"
image.save(root/"app/src/main/assets/app-icon.png")
image.save(root/"app/src/main/assets/fileport-icon.png")
for density,side in {"mdpi":48,"hdpi":72,"xhdpi":96,"xxhdpi":144,"xxxhdpi":192}.items():
 folder=res/f"mipmap-{density}";folder.mkdir(parents=True,exist_ok=True)
 icon=image.resize((side,side),Image.Resampling.LANCZOS)
 icon.save(folder/"ic_launcher.png",optimize=True)
 round_mask=Image.new("L",(side,side),0)
 ImageDraw.Draw(round_mask).ellipse((0,0,side-1,side-1),fill=255)
 icon.putalpha(ImageChops.multiply(icon.getchannel("A"),round_mask))
 icon.save(folder/"ic_launcher_round.png",optimize=True)
image.save(res/"mipmap-xxxhdpi/ic_launcher_512.png",optimize=True)
print("Generated Fileport Android launcher icons from Newdesign/logo.png")
