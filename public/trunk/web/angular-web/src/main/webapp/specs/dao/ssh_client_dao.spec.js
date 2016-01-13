describe('unit test:ssh client dao', function () {

    var sshClientDao;
    var RESOURCE;
    var server;
    var userParams =  {
        "publicKey": "aKey"
    };

    var userParms = "?publicKey=aKey";

    // inject the main module
    beforeEach(m('artifactory.dao'));

    // run this code before each case
    beforeEach(inject(function ($injector) {
        sshClientDao = $injector.get('SshClientDao');
        RESOURCE = $injector.get('RESOURCE');
        server = $injector.get('$httpBackend');
    }));

    it('sshClientDao should return a resource object', function () {
        expect(sshClientDao.name).toBe('Resource');
    });

    it('should fetch data', function() {
        server.expectPOST(RESOURCE.API_URL + RESOURCE.SSH_CLIENT).respond(200);
        sshClientDao.fetch({publicKey: 'aKey'});
        server.flush();
    });

    it('should update data', function() {
        server.expectPUT(RESOURCE.API_URL + RESOURCE.SSH_CLIENT).respond(200);
        sshClientDao.update({publicKey: 'aKey'});
        server.flush();
    });

});