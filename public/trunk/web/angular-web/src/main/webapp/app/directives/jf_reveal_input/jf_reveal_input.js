class jfRevealInputController {
	constructor($element) {
		this.$elementIcon = $element.find('i');
		this.tooltipText = 'Show ' + this.objectName;
	}
	
	updateInput() {
		let input = $(`#${this.inputId}`);
		let type = input.attr('type');
		if (type === 'text') {
			input.attr('type', 'password');
			this.$elementIcon.removeClass('icon-unview').addClass('icon-view');
			this.tooltipText = this.tooltipText.replace('Hide', 'Show');
		}
		else {
			input.attr('type', 'text');
			this.$elementIcon.removeClass('icon-view').addClass('icon-unview');
			this.tooltipText = this.tooltipText.replace('Show', 'Hide');
		}
	}
}

export function jfRevealInput() {
    return {
    	restrict: 'A',
		template: `<i class="icon icon-view icon-2x jf-reveal-input"
					  jf-tooltip="{{jfRevealInput.tooltipText}}"
		   			  ng-click="jfRevealInput.updateInput()"></i>`,
		controller: jfRevealInputController,
		controllerAs: 'jfRevealInput',
		bindToController: true,
		scope: {
			inputId: '@jfRevealInput',
			objectName: '@'
		}
    }
}