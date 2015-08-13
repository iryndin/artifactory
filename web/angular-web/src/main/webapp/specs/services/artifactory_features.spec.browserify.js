describe('ArtifactoryFeatures', () => {
	let ArtifactoryFeatures;

	beforeEach(m('artifactory.services', 'artifactory.dao'));
	function setup(versionID, isOnline) {	
		beforeEach(inject((_ArtifactoryFeatures_, FooterDao) => {
			ArtifactoryFeatures = _ArtifactoryFeatures_;
			spyOn(FooterDao, 'getInfo').and.returnValue({
				versionID: versionID,
				isAol: isOnline
			});			
		}));
	}

	describe('OSS on-premise', () => {
		setup("OSS", false);
	});

	describe('PRO on-premise', () => {
		setup("PRO", false);
	});

	describe('ENT on-premise', () => {
		setup("ENT", false);
	});

	describe('PRO online', () => {
		setup("PRO", false);
	});

	describe('ENT online', () => {
		setup("ENT", false);
	});

	describe('getAllowedLicense', () => {
		setup("ENT", false);
		it ('should return OSS', () => {
			expect(ArtifactoryFeatures.getAllowedLicense('Ruby')).toEqual("OSS");
		});
		it ('should return PRO', () => {
			expect(ArtifactoryFeatures.getAllowedLicense('NuGet')).toEqual("PRO");
		});
		it ('should return ENT', () => {
			expect(ArtifactoryFeatures.getAllowedLicense('highAvailability')).toEqual("ENT");
		});
	});

	describe('isAllowed', () => {
		// TBD
	});

	describe('isDisabled', () => {
		// TBD
	});

	describe('isHidden', () => {
		// TBD
	});

	describe('isAol', () => {
		// TBD
	});

	describe('getCurrentLicense', () => {
		// TBD
	});
});