import groovy.transform.Field

@Field
Map<String, List<String>> dependencyGraph = [
    "3.5": [
        "vertx-codegen"               : [],
        "vertx-core"                  : ["vertx-codegen"],

        // Rx
        "vertx-rx"   : ["vertx-core"],

        // Langs
        "vertx-lang-groovy"           : ["vertx-core"],
        "vertx-lang-js"               : ["vertx-core"],
        "vertx-lang-ruby"             : ["vertx-core"],
        "vertx-lang-kotlin"           : ["vertx-core", "vertx-rx"],

        // Service
        "vertx-service-factory"       : ["vertx-core"],
        "vertx-maven-service-factory" : ["vertx-service-factory"],
        "vertx-http-service-factory"  : ["vertx-service-factory"],

        // Utils
        "vertx-embedded-mongo-db"     : ["vertx-service-factory"],

        // Polyglot components
        "vertx-unit"                  : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-sql-common"            : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-bridge-common"         : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-mongo-client"          : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-jdbc-client"           : ["vertx-sql-common"],
        "vertx-auth"                  : ["vertx-jdbc-client", "vertx-mongo-client"],
        "vertx-web"                   : ["vertx-auth", "vertx-bridge-common", "vertx-unit"],
    ],
    "master": [
        "vertx-codegen"      : [],
        "vertx-core"         : [ "vertx-codegen" ],
        "vertx-unit"         : ["vertx-core"],
        "vertx-sql-common"   : ["vertx-core"],
        "vertx-jdbc-client"  : ["vertx-sql-common"],
        "vertx-auth"         : ["vertx-core", "vertx-jdbc-client", "vertx-mongo-client"],
        "vertx-mongo-client" : ["vertx-core"],
        "vertx-bridge-common": ["vertx-core"],
        "vertx-web"          : ["vertx-core", "vertx-auth", "vertx-bridge-common", "vertx-unit"],
    ]
]

// Perform a topological sort of the set according to the dependency graph
def topologicalVisit(String version, String id, List<String> sorted) {
    def dependencies = [] as Set
    for (entry in dependencyGraph[version]) {
        if (entry.value.contains(id)) {
            dependencies.add(entry.key)
        }
    }
    for (dependency in dependencies) {
        if (!sorted.contains(dependency)) {
            topologicalVisit(version, dependency, sorted)
        }
    }
    sorted.add(id)
}

def topologicalSort(String version, String id) {
    def sorted = [] as List
    topologicalVisit(version, id, sorted)
    def result = [] as List
    // Sort in correct order and omit first element (that is self)
    for (int i = sorted.size() - 2;i >= 0;i--) {
        result.add(sorted[i])
    }
    return result
}

// For testing purpose in IDE
def test() {
    def closure = topologicalSort("3.5", "vertx-codegen")
    println closure
}

test()

def call() {

    // Find if we have an existing job list in params
    def jobList = params.jobList
    if (jobList != null) {
        echo "Triggered by job list [$jobList]"
        if (jobList.length() > 0) {
            int idx = jobList.indexOf(',')
            def nextJob = idx == - 1 ? jobList : jobList.substring(0, idx)
            def nextJobList = jobList.substring(idx + 1)
            build job: nextJob, wait: false, parameters: [[$class: 'StringParameterValue', name: 'jobList', value: nextJobList]]
        }
    } else {
        int idx
        def job = "${env.JOB_NAME}"
        idx = job.lastIndexOf('/')
        if (idx == -1) {
            echo "Invalid job name ${env.JOB_NAME}"
            return
        }
        def version = job.substring(idx + 1)
        job = job.substring(0, idx)
        idx = job.lastIndexOf('/')
        if (idx == -1) {
            echo "Invalid job name ${env.JOB_NAME}"
            return
        }
        def id = job.substring(idx + 1)
        def prefix = job.substring(0, idx)

        def dependencies = topologicalSort(version, id)
        echo "${id} -> ${dependencies}"
        if (dependencies.size() > 0) {
            def nextJob = "$prefix/${dependencies[0]}/${version}"
            def nextJobList = ""
            for (int i = 1;i < dependencies.size();i++) {
                if (i > 1) {
                    nextJobList += ","
                }
                nextJobList += "$prefix/${dependencies[i]}/${version}"
            }
            build job: nextJob, wait: false, parameters: [[$class: 'StringParameterValue', name: 'jobList', value: nextJobList]]
        }
    }
}
