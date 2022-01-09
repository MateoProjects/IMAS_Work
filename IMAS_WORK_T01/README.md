# IMAS Work Team 1
## Authors
* Sergi Albiach
* Anna Garriga
* Benet Manzanares
* Ramon Mateo

## Execution guide
In order to execute our project, first **Java 1.8 SDK** and **Maven** are required. We recommend using *IntelliJ IDEA* as IDE, which includes or facilitates this dependencies.
After, a *mvn install* command need to be executed for building the project. At this moment, using the **pom.xml** file, *Maven* will be able to install the rest of
required software, highlighting **JADE 4.5.0** and **Weka 3.8.5**.

Once all dependencies are installed, the project can be executed with *Maven* using the **jade-classifiers** profile from the **pom.xml** file.
All the notifications, results and errors will be displayed at terminal.
**IMPORTANT**: The configuration file file which defines the dataset, classifiers and more is **settings.xml** and is placed at **src/main/resources** along with the **audit_risk.arff** file.
The configurations are the following:
* **dataset_filename**: Filename of the dataset to use, which is placed also in the resources folder
* **num_classifiers**: Number of classifiers to create
* **num_training_instances**: Number of instances from the dataset to be used for training
* **num_test_instances**: Number of instances from the dataset for the testing dataset.
* **num_test_subset_instances**: Number of instances from the testing dataset for creating the subset dataset to be used at testing.
* **num_test_attributes**: Number of attributes to select for the testing instances
* **num_instances_per_classifier**: Number of instances to use for training/validation at each classifier
* **num_validation_instances_per_classifier**: Number of instances of for validation at each classifier (the rest will be for training)
* **num_training_attributes_per_classifier**: Number of attributes to be used by each classifier for training