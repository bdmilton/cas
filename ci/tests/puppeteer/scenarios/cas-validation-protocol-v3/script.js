const puppeteer = require('puppeteer');
const cas = require('../../cas.js');
const http = require('http');
const httpCasClient = require('http-cas-client');
const assert = require('assert');

(async () => {
    const handler = httpCasClient({
        cas: 3,
        casServerUrlPrefix: 'https://localhost:8443/cas',
        serverName: 'http://localhost:8080'
    });
    console.log("Creating HTTP server for CAS client on port 8080");
    let server = await http.createServer(async (req, res) => {
        if(!await handler(req, res)) {
            return res.end();
        }
        const { principal } = req;
        console.log(principal);
        if (principal !== undefined) {
            assert(principal.user === "casuser");
            assert(principal.attributes.email === "casuser@apereo.org");
            assert(principal.attributes.username === "casuser");
            assert(principal.attributes.name === "CAS");
        }
        res.end();
    }).listen(8080);

    await server.on("listening", () => server.closeAllConnections());
    
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    await cas.goto(page, "http://localhost:8080");
    await cas.loginWith(page, "casuser", "Mellon");

    await browser.close();
    await process.exit(0);
})();
