const Ajv2019 = require('ajv/dist/2019')
const addFormats = require("ajv-formats")
const ajv = new Ajv2019()
addFormats(ajv)

const address = require('../registration/address.schema.1.0.0.json');
const country = require('../registration/country.schema.1.0.0.json');
const healthProfessional = require('../health-and-disability/health-professional.schema.1.0.0.json');
const healthCondition = require('../health-and-disability/health-condition.schema.1.0.0.json');
const dailyLiving = require('../health-and-disability/daily-living.schema.1.0.0.json');
const mobility = require('../health-and-disability/mobility.schema.1.0.0.json');
const healthAndDisabilitySchema = require('../health-and-disability/health-and-disability.schema.1.0.0.json');

const healthProfessionalsDetails = {
  fullName: 'Dr Seuss',
  profession: 'GP',
  phoneNumber: '01234123123',
  address: {
    line1: 'test',
    town: 'town',
    county: 'county',
    postcode: 'AB1 1AB',
    country: 'United Kingdom'
  },
  lastSeenMonth: '10',
  lastSeenYear: '2020'
}

const validDetails = 'abcdefghijklmnopqrstuvwxyz';

const validHealthCondition = {
  'healthCondition': 'one condition',
  'approxStartDate': 'about 12 months ago',
  'conditionDescription': validDetails
};

const validOtherDetails = {
  'conditionAffected': true,
  'otherDescription': validDetails,
}

const validate = ajv.addSchema(address)
    .addSchema(country)
    .addSchema(healthCondition)
    .addSchema(healthProfessional)
    .addSchema(dailyLiving)
    .addSchema(mobility)
    .compile(healthAndDisabilitySchema);

const minimumPip2Data = {
  health: {
    conditions: [ validHealthCondition ],
    'healthProfessionalsDetails': {
      includeHealthProfessionals: false,
    }
  },
  'dailyLivingActivity': {
    "preparingFood": { 'conditionAffected': false },
    "eatingDrinking": { 'conditionAffected': false },
    "manageTreatment": { 'conditionAffected': false },
    "washingBathing": { 'conditionAffected': false },
    "toiletIncontinence": { 'conditionAffected': false },
    "dressingUndressing": { 'conditionAffected': false },
    "commCognitive": { 'conditionAffected': false },
    "reading": { 'conditionAffected': false },
    "social": { 'conditionAffected': false },
    "manageMoney": { 'conditionAffected': false }
  },
  mobility: {
    'planningNavigate': { 'conditionAffected': false },
    'movingAround': { 'conditionAffected': false }
  },
  other: { conditionAffected: false }
}

const maximumPip2Data = {
  health: {
    conditions: [ validHealthCondition, validHealthCondition, validHealthCondition ],
    'healthProfessionalsDetails': {
      includeHealthProfessionals: true,
      professionals: [ healthProfessionalsDetails, healthProfessionalsDetails, healthProfessionalsDetails ],
    },
  },
  'dailyLivingActivity': {
    'preparingFood': { 'conditionAffected': true, 'preparingFoodDescription': validDetails },
    'eatingDrinking': { 'conditionAffected': true, 'useFeedingTube': 'No', 'eatingDrinkingDescription': validDetails },
    'manageTreatment': { 'conditionAffected': true, therapy: validDetails, 'manageTreatmentDescription': validDetails },
    'washingBathing': { 'conditionAffected': true, 'washingBathingDescription': validDetails },
    'toiletIncontinence': { 'conditionAffected': true, 'toiletIncontinenceDescription': validDetails },
    'dressingUndressing': { 'conditionAffected': true, 'dressingUndressingDescription': validDetails },
    'commCognitive': { 'conditionAffected': true, 'commCognitiveDescription': validDetails },
    'reading': { 'conditionAffected': true, 'readingDescription': validDetails },
    'social': { 'conditionAffected': true, 'socialDescription': validDetails },
    'manageMoney': { 'conditionAffected': true, 'manageMoneyDescription': validDetails }
  },
  mobility: {
    'planningNavigate': {
      'conditionAffected': true,
      'planningNavigateDescription': validDetails,
    },
    'movingAround': {
      'conditionAffected': true,
      'movingAroundDescription': validDetails,
      severity: {
        grade: 'varies',
        note: validDetails,
      }
    }
  },
  other: validOtherDetails,
}

const mixedResponsePip2Data = {
  health: {
    conditions: [ validHealthCondition ],
    'healthProfessionalsDetails': {
      includeHealthProfessionals: true,
      professionals: [ healthProfessionalsDetails ],
    },
  },
  'dailyLivingActivity': {
    'preparingFood': { 'conditionAffected': false },
    'eatingDrinking': { 'conditionAffected': false },
    'manageTreatment': { 'conditionAffected': true, therapy: validDetails, 'manageTreatmentDescription': validDetails },
    'washingBathing': { 'conditionAffected': false },
    'toiletIncontinence': { 'conditionAffected': false },
    'dressingUndressing': { 'conditionAffected': false },
    'commCognitive': { 'conditionAffected': true, 'commCognitiveDescription': validDetails },
    'reading': { 'conditionAffected': false },
    'social': { 'conditionAffected': true, 'socialDescription': validDetails },
    'manageMoney': { 'conditionAffected': false }
  },
  mobility: {
    'planningNavigate': {
      'conditionAffected': true,
      'planningNavigateDescription': validDetails,
    },
    'movingAround': {
      'conditionAffected': true,
      'movingAroundDescription': validDetails,
      severity: {
        grade: 'cannotMove',
      }
    }
  },
  other: { conditionAffected: false },
}

const validateHealthAndDisabilitySchema = () => {
  const errors = [];
  const validMinimumPip2 = validate(minimumPip2Data);
  if (!validMinimumPip2) errors.push(validate.errors);

  const validMaximumPip2 = validate(maximumPip2Data);
  if (!validMaximumPip2) errors.push(validate.errors);

  const validMixedResponsePip2 = validate(mixedResponsePip2Data);
  if (!validMixedResponsePip2) errors.push(validate.errors);

  return errors;
}

module.exports = validateHealthAndDisabilitySchema;
