const https = require('https');

https.get('https://developers.google.com/ml-kit/genai/prompt/android/get-started', (res) => {
  let data = '';

  res.on('data', (chunk) => {
    data += chunk;
  });

  res.on('end', () => {
    const lines = data.split('\n');
    lines.forEach((line, index) => {
      if (line.includes('implementation') && line.includes('mlkit')) {
        console.log(line);
      }
      if (line.includes('com.google.mlkit:')) {
         console.log(line);
      }
      if (line.includes('import com.google.mlkit')) {
         console.log(line);
      }
    });
  });
}).on("error", (err) => {
  console.log("Error: " + err.message);
});
