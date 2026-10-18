const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
assert.equal(fs.existsSync(path.join(root,'app/src/main/assets/music.js')),false);
for(const p of ['app/src/main/assets/index.html','app/src/main/assets/i18n.js','app/src/main/assets/brand-v30.css','app/src/main/java/com/fileport/android/MainActivity.java']){
 assert.doesNotMatch(read(p),/MusicPort|FileportMusic|music-offer|music-share|mediaPlayer|prepareMusic|scheduleMusic|data-tab="music"/i,'Orphan music reference in '+p);
}
assert.doesNotMatch(read('app/src/main/assets/app.js'),/FileportMusicTransport|sendMusic\(/);
assert.match(read('app/src/main/assets/app.js'),/Unsupported file metadata/);
assert.match(read('app/build.gradle'),/versionCode 35; versionName '3.0.4'/);
assert.match(read('app/src/main/java/com/fileport/android/GithubUpdater.java'),/getApkContentsSigners/);
assert.match(read('README.md'),/Source version:\*\* v3\.0\.4/);
console.log('File-only Android regression checks passed');
