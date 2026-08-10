# Schemas

This repository holds the schema for data layer for pip-apply. There 4 schemas, such as:

- eligibility
- registration
- health and disability
- motability

These may be broken down into numerous sub-schemas to facilitate reuse and aid understanding.

We also maintain different versions of the schemas.

## Registration Schema

This provides the schema for the PIP1, and references to 5 other schemas, which contain data relating to different sections of the PIP1. These may in turn reference other more detailed schemas.

## Health and Disability Schema

This provides the schema for the PIP2, and references to 4 other schemas, which contain data relating to different sections of the PIP2.

## Schema validation

The Ajv JSON schema validator is available in the utilities folder.

Within that folder is an index.js file that calls validators for 

- the Registration schema (validate-registration.js), and
  
- the Health and Disability schema (validate-health-and-disability.js).

These validators may need to be amended depending upon the changes made to the schemas, especially if new versions of schemas are introduced.

If new versions of the registration or health-and-disability schemas are created the index.js file will need to be changed.

To run the validators for both the registration schema and the Health-and-Disability (PIP2) schema

```bash
npm --prefix utilities run validate
```

## Using the schemas

The schemas are brought into services as submodules. 

