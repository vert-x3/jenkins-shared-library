import groovy.transform.Field

@Field
Map<String, List<String>> dependencyGraph = [
    "3.5": [
        "vertx-codegen"                 : [],
        "vertx-core"                    : ["vertx-codegen"],

        // Programming
        "vertx-rx"                      : ["vertx-core"],
        "vertx-reactive-streams"        : ["vertx-core"],
        "vertx-sync"                    : ["vertx-core"],

        // Langs
        "vertx-lang-groovy"             : ["vertx-core"],
        "vertx-lang-js"                 : ["vertx-core"],
        "vertx-lang-ruby"               : ["vertx-core"],
        "vertx-lang-kotlin"             : ["vertx-core", "vertx-rx"],

        // Service factory
        "vertx-service-factory"         : ["vertx-core"],
        "vertx-maven-service-factory"   : ["vertx-service-factory"],
        "vertx-http-service-factory"    : ["vertx-service-factory"],

        // Services
        "vertx-service-proxy"           : ["vertx-core", "vertx-auth"],
        "vertx-grpc"                    : ["vertx-core"],
        "vertx-client-services"         : ["vertx-mongo-client", "vertx-consul-client", "vertx-mail-client"],

        // Testing
        "vertx-unit"                    : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-junit5"                  : ["vertx-core"],

        // Legacy
        "vertx-jca"                     : ["vertx-core"],

        // Clustering
        "vertx-hazelcast"               : ["vertx-core", "vertx-web"],
        "vertx-infinispan"              : ["vertx-core", "vertx-web"],
        "vertx-zookeeper"               : ["vertx-core", "vertx-web"],
        "vertx-ignite"                  : ["vertx-core", "vertx-web"],

        // Messaging
        "vertx-proton"                  : ["vertx-unit"],
        "vertx-amqp-bridge"             : ["vertx-proton"],
        "vertx-stomp"                   : ["vertx-unit"],
        "vertx-camel-bridge"            : ["vertx-unit", "vertx-stomp"],
        "vertx-kafka-client"            : ["vertx-unit"],
        "vertx-mqtt"                    : ["vertx-unit"],
        "vertx-rabbitmq-client"         : ["vertx-unit"],

        // Bridge
        "vertx-bridge-common"           : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-tcp-eventbus-bridge"     : ["vertx-core", "vertx-bridge-common"],

        // Data
        "vertx-embedded-mongo-db"       : ["vertx-service-factory"],
        "vertx-sql-common"              : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-mongo-client"            : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-redis-client"            : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-jdbc-client"             : ["vertx-sql-common"],
        "vertx-mysql-postgresql-client" : ["vertx-sql-common"],
        "vertx-consul-client"           : ["vertx-unit", "vertx-web"],

        // Mail
        "vertx-mail-client"             : ["vertx-unit"],

        // Security
        "vertx-auth"                    : ["vertx-jdbc-client", "vertx-mongo-client"],

        // Microservices
        "vertx-circuit-breaker"         : ["vertx-rx", "vertx-lang-groovy", "vertx-lang-js", "vertx-lang-ruby", "vertx-lang-kotlin"],
        "vertx-service-discovery"       : ["vertx-hazelcast", "vertx-web", "vertx-jdbc-client", "vertx-redis-client", "vertx-mongo-client", "vertx-consul-client", "vertx-service-proxy"],
        "vertx-health-check"            : ["vertx-web", "vertx-auth", "vertx-service-discovery", "vertx-unit"],
        "vertx-config"                  : ["vertx-web", "vertx-redis-client", "vertx-consul-client"],

        // Web
        "vertx-web"                     : ["vertx-auth", "vertx-bridge-common", "vertx-unit"],

        // Shell
        "vertx-shell"                   : ["vertx-unit", "vertx-auth", "vertx-web"],

        // Metrics
        "vertx-dropwizard-metrics"      : ["vertx-shell", "vertx-service-factory"],
        "vertx-micrometer-metrics"      : ["vertx-unit", "vertx-web"]
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

// For testing purpose - check we don't have cycle
def test() {
    def t = dependencyGraph["3.5"]
    for (entry in t) {
        def closure = topologicalSort("3.5", entry.key)
    }
}

test()

def call() {

    // Find if we have an existing job list in params
    def jobList = params.jobList
    if (jobList != null) {
        echo "Triggered by job list [$jobList]"
        if (jobList.length() > 0) {
            int idx = jobList.indexOf(',')
            def nextJob
            def nextJobList
            if (idx == -1) {
                nextJob = jobList
                nextJobList = ""
            } else {
                nextJob = jobList.substring(0, idx)
                nextJobList = jobList.substring(idx + 1)
            }
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
