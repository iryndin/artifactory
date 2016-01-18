export class ChangePasswordController {

    constructor(UserDao, $state, $stateParams) {
        this.$state = $state;
        this.userDao = UserDao.getInstance();
        this.fields = {};
        this.username = $stateParams.username;
    }

    passwordsMatch() {
        return this.fields.newPassword === this.fields.retypeNewPassword;
    }

    change() {
        this.userDao.changePassword({},{
            userName: this.username,
            oldPassword: this.fields.oldPassword,
            newPassword1: this.fields.newPassword,
            newPassword2: this.fields.retypeNewPassword
        }).$promise.then((res)=>{
            if (res.status === 200) {
                this.$state.go('login');
            }

        })
    }
}
