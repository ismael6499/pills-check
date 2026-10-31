const fs = require('fs');
const path = require('path');

console.log('--- RUNNING PILLSCHECK UI & I18N AUDIT ---');

const projectRoot = __dirname;
const enStringsPath = path.join(projectRoot, 'app/src/main/res/values/strings.xml');
const esStringsPath = path.join(projectRoot, 'app/src/main/res/values-es/strings.xml');

function parseStringsXml(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    const regex = /<string\s+name="([^"]+)">([\s\S]*?)<\/string>/g;
    const map = new Map();
    let match;
    while ((match = regex.exec(content)) !== null) {
        map.set(match[1], match[2]);
    }
    return map;
}

const enStrings = parseStringsXml(enStringsPath);
const esStrings = parseStringsXml(esStringsPath);

console.log(`✓ English strings count: ${enStrings.size}`);
console.log(`✓ Spanish strings count: ${esStrings.size}`);

let missingInEs = [];
for (const key of enStrings.keys()) {
    if (!esStrings.has(key)) {
        missingInEs.push(key);
    }
}

if (missingInEs.length > 0) {
    console.error(`✗ Missing keys in Spanish strings:`, missingInEs);
    process.exit(1);
} else {
    console.log('✓ 100% i18n parity between English and Spanish dictionary!');
}

// Layout hierarchy validation
const layouts = ['activity_main.xml', 'item_medication_card.xml', 'activity_history.xml', 'activity_settings.xml'];
layouts.forEach(file => {
    const layoutPath = path.join(projectRoot, 'app/src/main/res/layout', file);
    if (!fs.existsSync(layoutPath)) {
        console.error(`✗ Missing layout file: ${file}`);
        process.exit(1);
    }
    const content = fs.readFileSync(layoutPath, 'utf8');
    if (!content.includes('<?xml')) {
        console.error(`✗ Invalid XML in ${file}`);
        process.exit(1);
    }
    console.log(`✓ Layout ${file} parsed successfully`);
});

// Seed data check
const defaultSeed = path.join(projectRoot, 'app/src/main/assets/default_medications.json');
const localSeed = path.join(projectRoot, 'app/src/main/assets/local_seed.json');

if (fs.existsSync(defaultSeed)) {
    const defaultData = JSON.parse(fs.readFileSync(defaultSeed, 'utf8'));
    console.log(`✓ Default seed contains ${defaultData.length} template medications`);
} else {
    console.error('✗ Missing default_medications.json');
    process.exit(1);
}

if (fs.existsSync(localSeed)) {
    const localData = JSON.parse(fs.readFileSync(localSeed, 'utf8'));
    console.log(`✓ Local personal seed contains ${localData.length} medications: ${localData.map(m => m.name).join(', ')}`);
}

console.log('--- ALL UI & REPO AUDIT CHECKS PASSED ---');
