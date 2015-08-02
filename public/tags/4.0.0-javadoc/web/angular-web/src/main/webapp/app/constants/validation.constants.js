export default function (name) {
    return angular.extend(angular.copy(commonMessages), customMessages[name]);
}

const commonMessages = {
    "required": "You must fill this field",
    "unique": "This value must be unique",
    "validator": "This value is invalid",
    "email": "Please enter a valid email",
    "url": "Please enter a valid url",
    "number": "Please enter an Integer",
    "min": "Value too low",
    "max": "Value too high",
    "minlength": "Value too short",
    "maxlength": "Value too long",
    "invalidCron": "The cron expression is invalid",
    "pastCron": "The next run time is in the past",
    "uniqueId": "Key is already used",
    "name": "Invalid name",
    "xmlName": "Invalid name",
    "integerValue": "Value must be an integer number"
};

const customMessages = {
    "adminGeneral": {
        "min": "Value must be between 0 and 2,147,483,647",
        "max": "Value must be between 0 and 2,147,483,647",
        "dateFormatExpression": "Please enter a valid date format"
    },
    "adminBackup": {
        "name": "Invalid backup name",
        "xmlName": "Invalid backup name"
    },
    "adminMail": {
        "min": "Port must be between 1 and 65535",
        "max": "Port must be between 1 and 65535"
    },
    "proxies": {
        "min": "Port must be between 1 and 65535",
        "max": "Port must be between 1 and 65535"
    },
    "users": {
        "validator": "Passwords do not match",
        "minlength": "Password must be more than 4 characters",
        "maxlength": "Username must be less than 64 characters"
    },
    "maintenance": {
        "min": "Value must be between 0 and 99",
        "max": "Value must be between 0 and 99",
    },
    "crowd": {
        "min": "Value must be between 0 and 9999999999999",
        "max": "Value must be between 0 and 9999999999999",
        "url": "Please enter a valid url"
    },
    "ldapSettings": {
        "ldapUrl": "LDAP url must be a valid ldap url, e.g: ldap://somehost.com"
    },
    "gridFilter": {
        "maxlength": "Filter is too long"
    },
    "properties": {
        "validCharacters": "Name cannot include the following characters: *<>~!@#$%^&()+=-{}[];,`/\\",
        "predefinedValues": "Must supply predefined values for the selected type"
    },
    "repoLayouts": {
        "pathPattern": "Pattern must at-least contain the tokens 'module', 'baseRev' and 'org' or 'orgPath'."
    },
    "bintray": {
        "required": "Cannot leave apiKey / userName blank"
    },
    "licenses": {
        "validateLicense": "License name contains illegal characters"
    }
};