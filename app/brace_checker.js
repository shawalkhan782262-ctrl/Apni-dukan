const fs = require('fs');
const content = fs.readFileSync('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'utf8');
const lines = content.split('\n');
let level = 0;
lines.forEach((line, index) => {
    let oldLevel = level;
    for (let char of line) {
        if (char === '{') level++;
        if (char === '}') level--;
    }
    // Print lines where level changes, or important keywords
    if (line.includes('fun DashboardScreen') || line.includes('fun KpiCard') || line.includes('fun SaleLedgerItemRow') || line.includes('LazyColumn') || level < 0) {
        console.log(`Line ${index + 1}: level ${oldLevel} -> ${level} | ${line.trim().substring(0, 70)}`);
    }
});
console.log('Final level:', level);
