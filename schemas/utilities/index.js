const validateRegistrationSchema = require("./validate-registration");
const validateHealthAndDisabilitySchema = require("./validate-health-and-disability");

validateRegistrationSchema("1.0.0");
validateRegistrationSchema("1.1.0");
validateRegistrationSchema("1.2.0");
validateRegistrationSchema("1.3.0");
validateRegistrationSchema("1.4.0");
validateRegistrationSchema("1.5.0");
validateHealthAndDisabilitySchema();
