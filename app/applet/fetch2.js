const https = require('https');

https.get('https://developers.google.com/ml-kit/genai/prompt/android/get-started', (res) => {
  let data = '';

  res.on('data', (chunk) => {
    data += chunk;
  });

  res.on('end', () => {
    const lines = data.split('\n');
    lines.forEach((line) => {
      if (line.includes('com.google.mlkit:')) {
         console.error("DEP: " + line.trim());
      }
      if (line.includes('GenerativeModel')) {
         console.error("CODE: " + line.trim());
      }
    });
  });
}).on("error", (err) => {
  console.error("Error: " + err.message);
});
