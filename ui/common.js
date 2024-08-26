const system = {imports: []}

function emit(component, type, detail = {}) {
    const event = new CustomEvent(`${component}:${type}`,
        {bubbles: true, cancelable: true, detail: detail});
    return dispatchEvent(event);
}

async function importComponent(componentName, html, ctor) {
    if (system.imports.indexOf(componentName) === -1) {
        async function loadHTMLFragment(url, elementId) {
            return fetch(url)
                .then(response => response.text())
                .catch(error => console.error('Error loading HTML fragment:', error));
        }
        const htmlMarkup = await loadHTMLFragment(html)
        const e = document.createElement('div')
        e.innerHTML = htmlMarkup
        document.body.appendChild(e)
        window.customElements.define(componentName, ctor)
        console.debug('importing ' + componentName + ' with HTML template ' + html)
        system.imports.push(componentName)
    }//
    else {
        console.debug(`already imported ${componentName}, skipping..`)
    }
}