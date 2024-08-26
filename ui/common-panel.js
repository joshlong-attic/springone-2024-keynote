class CommonPanelComponent extends HTMLElement {


    static get observedAttributes() {
        return ['border-color', 'background-color']
    }

    constructor() {
        super();
        this.iconsPlaced = false
        this.icons = ['assets/ui-icons-paw-1.png', "assets/ui-icons-paw-2.png", "assets/ui-icons-bone-1.png", "assets/ui-icons-heart-1.png"]
        this.attachShadow({mode: 'open'})
        const cloneNode = document.getElementsByClassName('common-panel-template')[0].content.cloneNode(true);
        this.shadowRoot.append(cloneNode)
    }

    attributeChangedCallback(property, oldValue, newValue) {

        if (oldValue === newValue) return;
        this [property] = newValue

        this.doUpdateComponent()
    }


    connectedCallback() {
        this.doUpdateComponent()
    }


    doUpdateComponent() {

        const bgColor = this.getAttribute('background-color')
        const borderColor = this.getAttribute('border-color')

        const commonPanel = this.shadowRoot.querySelector('.common-panel');
        if (bgColor !== null) {
            commonPanel.style.backgroundColor = bgColor
        }
        if (borderColor !== null) {
            commonPanel.style.borderColor = borderColor
        }

        if (this.iconsPlaced === false) {

            function randomIndexFor(array) {
                return Math.floor(Math.random() * array.length)
            }

            const imgs = this.icons
                .map(url => {
                    const img = document.createElement('img')
                    img.setAttribute('src', url)
                    img.classList.add('icon')
                    return img
                })

            const corners = [
                {top: -10, left: -10}, // tl
                {top: -10, right: -10}, // tr
                {bottom: -10, left: -10}, //  bl
                {bottom: -10, right: -10}, //  br
            ]

            const element = commonPanel
            for (let i = 0; i < imgs.length; i++) {
                const img = imgs [i]
                const corner = corners.splice(randomIndexFor(corners), 1) [0]
                for (let p in corner) {
                    const position = corner[p];

                    img.style[p] = position + 'px'
                    const randomRotation = (Math.random() * 90) - 45;
                    img.style.transform = `rotate(${randomRotation}deg)`;
                }
                element.append(img)
            }


            this.iconsPlaced = true
        }


    }

}

addEventListener('load', async e => await importComponent('common-panel', 'common-panel.html', CommonPanelComponent))
