export function jfDisableFeature(ArtifactoryFeatures) {
	return {
		restrict: 'A',
        link: function ($scope, $element, $attrs, $timeout) {
			let feature = $attrs.jfDisableFeature;
			let currentLicense = ArtifactoryFeatures.getCurrentLicense();
			if (!feature) return;
			if (ArtifactoryFeatures.isHidden(feature)) {
				$($element).hide();
			}
			else if (ArtifactoryFeatures.isDisabled(feature)) {
				if (currentLicense === "OSS") {
                    setTimeout(()=> {
                        $($element).find("*").attr('disabled', true)
                    }, 500);
					let license = ArtifactoryFeatures.getAllowedLicense(feature);
					// Add the correct class:
					$($element).addClass('license-required-' + license);
					$($element).addClass('license-required');

					// Add a tooltip with link to the feature page:
					let featureName = ArtifactoryFeatures.getFeatureName(feature);
					let featureLink = ArtifactoryFeatures.getFeatureLink(feature);
					
					$($element).tooltipster({
		                contentAsHTML : 'true',
		                trigger: 'hover',
		                onlyOne: 'true',
		                interactive: 'true',
		                interactiveTolerance: 150,
		                position: 'top',
		                content: `Learn more about the <a href="${featureLink}" target="_blank">${featureName}</a> feature`
		            });					
				}
				else {
					$($element).hide();
				}
			}
		}
	}
}