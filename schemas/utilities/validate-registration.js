const Ajv2019 = require('ajv/dist/2019');
const addFormats = require("ajv-formats");

const REGISTRATION_SCHEMA_1_0_0 = '../registration/registration.schema.1.0.0.json';
const REGISTRATION_SCHEMA_1_1_0 = '../registration/registration.schema.1.1.0.json';
const REGISTRATION_SCHEMA_1_2_0 = '../registration/registration.schema.1.2.0.json';
const REGISTRATION_SCHEMA_1_3_0 = '../registration/registration.schema.1.3.0.json';
const REGISTRATION_SCHEMA_1_4_0 = '../registration/registration.schema.1.4.0.json';
const REGISTRATION_SCHEMA_1_5_0 = '../registration/registration.schema.1.5.0.json';

let registrationSchema;
const address = require('../registration/address.schema.1.0.0.json');
const country = require('../registration/country.schema.1.0.0.json');
const nationality = require('../registration/nationality.schema.1.0.0.json');
const nationality_1_1_0 = require('../registration/nationality.schema.1.1.0.json');
const nationality_1_2_0 = require('../registration/nationality.schema.1.2.0.json');
const eeaNationality = require('../registration/eea-nationality.schema.1.0.0.json');
const restOfWorldNationality = require('../registration/rest-of-world-nationality.schema.1.0.0.json');
const restOfWorldNationality_1_1_0 = require('../registration/rest-of-world-nationality.schema.1.1.0.json');
const restOfWorldNationality_1_2_0 = require('../registration/rest-of-world-nationality.schema.1.2.0.json');
const additionalSupport = require('../registration/additional-support.schema.1.0.0.json');
const personalDetails = require('../registration/personal-details.schema.1.0.0.json');
const personalDetails_1_1_0 = require('../registration/personal-details.schema.1.1.0.json');
const personalDetails_1_2_0 = require('../registration/personal-details.schema.1.2.0.json');
const residenceAndPresenceSchema = require('../registration/residence-and-presence.schema.1.0.0.json');
const residenceAndPresenceSchema_1_1_0 = require('../registration/residence-and-presence.schema.1.1.0.json');
const residenceAndPresenceSchema_1_2_0 = require('../registration/residence-and-presence.schema.1.2.0.json');
const aboutYourHealth = require('../registration/about-your-health.schema.1.0.0.json');
const aboutYourHealth_1_1_0 = require('../registration/about-your-health.schema.1.1.0.json');
const aboutYourHealth_1_2_0 = require('../registration/about-your-health.schema.1.2.0.json');
const motabilityScheme = require('../registration/motability-scheme.schema.1.0.0.json');
const healthProfessionalsDetails = require('../registration/health-professionals-details.schema.1.0.0.json');
const healthProfessionalsDetails_1_1_0 = require('../registration/health-professionals-details.schema.1.1.0.json');
const hospitalHospiceOrCarehome = require('../registration/hospital-hospice-or-carehome.schema.1.0.0.json');
const hospitalHospiceOrCarehome_1_1_0 = require('../registration/hospital-hospice-or-carehome.schema.1.1.0.json');

const validName = 'abcdefghijklmnopqrstuvwxyz';
const validNino = 'AB123123A';
const validDate = '2001-01-01';
const validPhoneNumber = '07123124123';
const validHealthCondition = { 'condition': 'one condition' };

const fullAddress = {
  line1: 'test',
  line2: 'district',
  town: 'town',
  county: 'county',
  postcode: 'AB1 1AB',
  country: 'United Kingdom'
}

const basicAddress = {
  line1: 'test',
  town: 'town',
  county: 'county',
  postcode: 'AB1 1AB',
  country: 'United Kingdom'
}

const healthCareProfessional = {
  name: validName,
  profession: 'GP',
  phoneNumber: validPhoneNumber,
  address: fullAddress,
  lastContact: 'last week'
}

const residenceAndPresence = {
  nationality: 'Angolan',
  residentBeforeBrexit: 'Don\'t know',
  inUkTwoOutOfThreeYears: 'Yes',
  receivingPensionsOrBenefitsFromEEA: true,
  payingInsuranceEEA: true,
};

const otherAlternateFormats = {
  formatType: "other",
  otherOptions: "email",
  alternateFormatAdditionalInfo: "tester@test.com",
}

const getFullDataSet = (schemaVersion) => {
  let fullDataSet = {
    additionalSupport: {
      helpCommunicating: true,
      helpCompletingLetters: true,
      helperDetails: {
        "surname": validName,
        "firstname": validName,
      },
    },
    personalDetails: {
      surname: validName,
      firstname: validName,
      nino: validNino,
      dob: validDate,
      address: fullAddress,
      alternativeAddress: fullAddress,
      contact: {
        mobileNumber: validPhoneNumber,
        alternativeNumber: validPhoneNumber,
        textphone: validPhoneNumber,
        smsUpdates: true,
      },
      alternateFormat: otherAlternateFormats,
    },
    residenceAndPresence: residenceAndPresence,
    aboutYourHealth: {
      healthConditions: [ validHealthCondition, validHealthCondition, validHealthCondition ],
      hcpShareConsent: false,
      healthProfessionalsDetails1: healthCareProfessional,
      hospitalHospiceOrCarehome: {
        accommodationType: "hospice",
        accommodationName: "Ramkin Residence",
        admissionDate: validDate,
        address: fullAddress
      }
    }
  }
  if (schemaVersion === '1.0.0' || schemaVersion === '1.1.0') {
    fullDataSet.aboutYourHealth.hcpContactConsent = false;
  }
  if (schemaVersion === '1.1.0') {
    fullDataSet.personalDetails.bankDetails = {
      enterBankDetails: 'Yes',
      accountName: 'my account',
      accountNumber: '12345678',
      sortCode: '123456',
      rollNumber: 'abc-123/x',
    };
    fullDataSet.motabilityScheme = {
      receiveMotabilityInformation: 'Yes',
    };
  }
  return fullDataSet;
}

const getMinimumDataSet = (schemaVersion) => {
  let minimumDataSet = {
    additionalSupport: {
      helpCommunicating: true,
      helpCompletingLetters: false,
    },
    personalDetails: {
      surname: validName,
      firstname: validName,
      nino: validNino,
      dob: validDate,
      address: basicAddress,
      contact: {
        mobileNumber: validPhoneNumber,
        smsUpdates: true,
      },
    },
    residenceAndPresence: residenceAndPresence,
    aboutYourHealth: {
      healthConditions: [],
      hcpShareConsent: false,
      healthProfessionalsDetails1: healthCareProfessional,
    }
  }
  if (schemaVersion === '1.0.0' || schemaVersion === '1.1.0') {
    minimumDataSet.aboutYourHealth.hcpContactConsent = false;
  }
  if (schemaVersion === '1.1.0') {
    minimumDataSet.personalDetails.bankDetails = {
      enterBankDetails: 'No',
    };
  }
  return minimumDataSet;
}

const validateRegistrationSchema = (schemaVersion) => {
  const ajv = new Ajv2019();
  addFormats(ajv);
  let validate;
  switch (schemaVersion) {
    case '1.5.0': {
      registrationSchema = require(REGISTRATION_SCHEMA_1_5_0);
      validate = ajv.addSchema(address)
        .addSchema(country)
        .addSchema(eeaNationality)
        .addSchema(restOfWorldNationality_1_2_0)
        .addSchema(nationality_1_2_0)
        .addSchema(additionalSupport)
        .addSchema(personalDetails_1_1_0)
        .addSchema(personalDetails_1_2_0)
        .addSchema(residenceAndPresenceSchema_1_2_0)
        .addSchema(healthProfessionalsDetails_1_1_0)
        .addSchema(hospitalHospiceOrCarehome_1_1_0)
        .addSchema(aboutYourHealth_1_2_0)
        .addSchema(motabilityScheme)
        .compile(registrationSchema);
      break;
    }
    case '1.4.0': {
      registrationSchema = require(REGISTRATION_SCHEMA_1_4_0);
      validate = ajv.addSchema(address)
        .addSchema(country)
        .addSchema(eeaNationality)
        .addSchema(restOfWorldNationality_1_1_0)
        .addSchema(nationality_1_1_0)
        .addSchema(additionalSupport)
        .addSchema(personalDetails_1_1_0)
        .addSchema(personalDetails_1_2_0)
        .addSchema(residenceAndPresenceSchema_1_1_0)
        .addSchema(healthProfessionalsDetails_1_1_0)
        .addSchema(hospitalHospiceOrCarehome_1_1_0)
        .addSchema(aboutYourHealth_1_2_0)
        .addSchema(motabilityScheme)
        .compile(registrationSchema);
      break;
    }
    case '1.3.0': {
      registrationSchema = require(REGISTRATION_SCHEMA_1_3_0);
      validate = ajv.addSchema(address)
        .addSchema(country)
        .addSchema(eeaNationality)
        .addSchema(restOfWorldNationality_1_1_0)
        .addSchema(nationality_1_1_0)
        .addSchema(additionalSupport)
        .addSchema(personalDetails_1_1_0)
        .addSchema(residenceAndPresenceSchema_1_1_0)
        .addSchema(healthProfessionalsDetails)
        .addSchema(hospitalHospiceOrCarehome)
        .addSchema(aboutYourHealth_1_1_0)
        .addSchema(motabilityScheme)
        .compile(registrationSchema);
      break;
    }
    case '1.2.0': {
      registrationSchema = require(REGISTRATION_SCHEMA_1_2_0);
      validate = ajv.addSchema(address)
        .addSchema(country)
        .addSchema(eeaNationality)
        .addSchema(restOfWorldNationality)
        .addSchema(nationality)
        .addSchema(additionalSupport)
        .addSchema(personalDetails_1_1_0)
        .addSchema(residenceAndPresenceSchema)
        .addSchema(healthProfessionalsDetails)
        .addSchema(hospitalHospiceOrCarehome)
        .addSchema(aboutYourHealth_1_1_0)
        .addSchema(motabilityScheme)
        .compile(registrationSchema);
      break;
    }
    case '1.1.0': {
      registrationSchema = require(REGISTRATION_SCHEMA_1_1_0);
      validate = ajv.addSchema(address)
        .addSchema(country)
        .addSchema(eeaNationality)
        .addSchema(restOfWorldNationality)
        .addSchema(nationality)
        .addSchema(additionalSupport)
        .addSchema(personalDetails)
        .addSchema(residenceAndPresenceSchema)
        .addSchema(healthProfessionalsDetails)
        .addSchema(hospitalHospiceOrCarehome)
        .addSchema(aboutYourHealth)
        .addSchema(motabilityScheme)
        .compile(registrationSchema);
      break;
    }
    default: {
      registrationSchema = require(REGISTRATION_SCHEMA_1_0_0);
      validate = ajv.addSchema(address)
        .addSchema(country)
        .addSchema(eeaNationality)
        .addSchema(restOfWorldNationality)
        .addSchema(nationality)
        .compile(registrationSchema);
    }
  }

  const errors = [];

  const valid = validate(getMinimumDataSet(schemaVersion));
  if (!valid) errors.push(validate.errors);

  const validFullDataSet = validate(getFullDataSet(schemaVersion));
  if (!validFullDataSet) errors.push(validate.errors);

  return errors;
}

module.exports = validateRegistrationSchema;
