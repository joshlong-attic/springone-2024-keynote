class DogPanelComponent extends HTMLElement {

    constructor() {
        super();
        this.initialized = false

        this.attachShadow({mode: 'open'})

        const cloneNode = document.getElementsByClassName('dog-panel-template')[0].content.cloneNode(true);
        this.shadowRoot.append(cloneNode)
    }

    attributeChangedCallback(property, oldValue, newValue) {
        if (oldValue === newValue) return;
        this [property] = newValue

        this.updateComponent();
    }

    getWebComponentCssRuleAttribute(selector, attr) {
        const shadow = this.shadowRoot
        for (let cssi = 0; cssi < shadow.styleSheets.length; cssi++) {
            const sheet = shadow.styleSheets.item(cssi)
            for (let i = 0; i < sheet.cssRules.length; i++) {
                if (sheet.cssRules[i].selectorText === selector) {
                    return sheet.cssRules[i].style [attr];
                }
            }
        }
        return null
    }

    copyButtonStyles(externalStylesheets, styleElement) {
        for (let s = 0; s < externalStylesheets.length; s++) {
            const ss = externalStylesheets [s].sheet
            // Copy CSS rules for buttons from the external stylesheet
            for (let i = 0; i < ss.cssRules.length; i++) {
                const rule = ss.cssRules[i];
                // console.log('rule: '  ,rule)
                if (rule.selectorText && rule.selectorText.includes('button')) {
                    // console.log('appending ' + rule.cssText)
                    styleElement.append(rule.cssText)
                }
            }
        }
    }

    updateComponent() {


        // variant, based on attributes
        this.shadowRoot.querySelector('.dog-name').innerText = this.name

        const owner = this.getAttribute('owner');
        const imageUrl = this.getAttribute('image-url');
        const element = this.shadowRoot.querySelector('.common-panel-root');
        const gender = this.getAttribute('gender') === 'f' ? 'f' : 'm';
        const rule = 'dog-gender-' + gender;
        element.classList.add(rule);
        const existingStyleElements = document.getElementsByTagName('style');
        const style = document.createElement('style');
        this.shadowRoot.appendChild(style);
        this.copyButtonStyles(existingStyleElements, style);

        const borderColorFromRule = this.getWebComponentCssRuleAttribute('.' + rule, 'borderColor')
        const bgColorFromRule = this.getWebComponentCssRuleAttribute('.' + rule, 'backgroundColor')

        function colors(border, bg) {
            if (border) {
                element.setAttribute('border-color', border)
            }
            if (bg) {
                element.setAttribute('background-color', bg)
            }
        }

        colors(borderColorFromRule, bgColorFromRule)

        this.shadowRoot.querySelector('.dog-birthday').innerText = this.dob

        this.shadowRoot.querySelector('.dog-picture').style.backgroundImage = `url("${imageUrl}")`


        if (owner && owner.trim() !== '' && owner.trim() !== 'null') {

            function transformForAdoptionStatus(owner, element) {
                element.classList.add('adopted')
            }

            const elementsByClassName = Array.from(this.shadowRoot.querySelectorAll('.dog-panel'));
            elementsByClassName.forEach(e => transformForAdoptionStatus(owner, e))


            const rootStyles = getComputedStyle(document.documentElement);

            // Read the value of the CSS variable
            const color = rootStyles.getPropertyValue('--color-orange').trim();
            colors(color, color)

            this.shadowRoot.querySelectorAll('.adopt-button')
                .forEach((e, k) => e.style.display = 'none')
            this.shadowRoot.querySelectorAll('.dog-owner').forEach((e, k) => {
                e.innerText = 'Adopted by ' + owner + '!'
                e.style.display = 'block'
            })
        }

        if (!this.initialized) {
            // invariant dom modifications
            this.shadowRoot.querySelectorAll('.adopt-button').forEach((el, key) => {
                el.addEventListener('click', evt => {
                    emit('dog-panel', 'adoption-initiated', {dogId: parseInt(this.getAttribute('dog-id'))})
                })
            })
            const slot = document.createElement('slot')
            this.shadowRoot.querySelector('.dog-description').append(slot)
            this.initialized = true
        }

    }

    async connectedCallback() {
        this.updateComponent()
    }

    static get observedAttributes() {
        return ['name', 'dob', 'image-url', 'gender', 'dog-id', 'owner']
    }


}

addEventListener('load', async e => await importComponent('dog-panel', 'dog-panel.html', DogPanelComponent))



