class Session {

    async me() {
        return (await (await fetch('/api/me')).json())['name']
    }
}

class Dogs {


    async help(query) {
        const response = await fetch('/api/assistant?question=' + encodeURIComponent(query), {
            method: 'GET',
            headers: {'Content-Type': 'application/json'},
        });
        return await response.json();
    }


    async adopt(dog) {
        const body = {'name':await api.session.me() };
        console.log('body' ,body)
        await fetch(`/api/dogs/${dog.id}/adoptions`, {
            method: 'POST',
            body: JSON.stringify(body),
            headers: {'Content-Type': 'application/json'},
        });

    }

    async read() {
        const response = await fetch('/api/dogs', {
            method: 'GET',
            headers: {'Content-Type': 'application/json'},
        });
        return await response.json();
    }
}

class ApiClient {

    dogs = new Dogs()

    session = new Session()


}

// console.log('exporting the windows.api...')
window.api = new ApiClient()
