"""Install approved Newdesign assets from the pinned Fileport PC source commit.
These are build-time downloads only; Android apps use local packaged PNGs.
"""
from pathlib import Path
from urllib.request import Request, urlopen
from hashlib import sha256

root=Path(__file__).resolve().parents[1]
dest=root/"app/src/main/assets/branding"
dest.mkdir(parents=True,exist_ok=True)
revision="52aafaa40c4ffec0c8de0d98c2e54102df5603a1"
url=f"https://raw.githubusercontent.com/s3rchan/Fileport-PC/{revision}/src/branding/"
files={
 "logo.png":"5b84abfd209bf163adde477e63da9a53588e4f6c0d97f35a14e065419a13fe50",
 "logo2.png":"c685ec09db98de9496e9cc2ab49615eba5706a0ab0c8e26929dbab292c2b0241",
 "logo5.png":"5848a11b64a7634f3526628b8defe89b9bd5e61b0f0fa317111168ded2b2b65a",
 "logo.detail.png":"a17984844e8490f737cbe26ceda4411ccc14b8ad3b2f5a0eb8794803ece6cb7a",
}
for name,expected in files.items():
 path=dest/name
 if path.exists() and sha256(path.read_bytes()).hexdigest()==expected:continue
 with urlopen(Request(url+name,headers={"User-Agent":"Fileport-brand-build/1.0"}),timeout=45) as response:
  data=response.read()
 if sha256(data).hexdigest()!=expected or not data.startswith(b"\x89PNG\r\n\x1a\n"):
  raise RuntimeError(f"Official logo verification failed: {name}")
 path.write_bytes(data)
 print("Packaged",name)
