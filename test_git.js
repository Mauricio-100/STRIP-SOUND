const fs = require('fs');
const path = require('path');

function walk(dir) {
    let results = [];
    try {
        const list = fs.readdirSync(dir);
        list.forEach(file => {
            const fullPath = path.join(dir, file);
            let stat;
            try {
                stat = fs.statSync(fullPath);
            } catch (e) {
                return;
            }
            if (stat && stat.isDirectory()) {
                results = results.concat(walk(fullPath));
            } else {
                if (file.endsWith('.kt') || file.endsWith('.java')) {
                    results.push(fullPath);
                }
            }
        });
    } catch(e) {}
    return results;
}

try {
    console.log("Searching app/build for Kotlin or Java files...");
    const files = walk('app/build');
    console.log(`Found ${files.length} files.`);
    files.forEach(f => {
        if (!f.includes('/R.java') && !f.includes('/BuildConfig.java')) {
            console.log(f);
        }
    });
} catch (e) {
    console.error(e.message);
}
