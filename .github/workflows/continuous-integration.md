name: Continuous integration
on:
  pull_request:
  push:
    branches-ignore:
      - 'dependabot/**'
concurrency: 
  group: ${{ github.ref }}
  cancel-in-progress: true
jobs:
  build_gradle:
    name: "JDK ${{ matrix.java }} on ${{ matrix.os }}"
    strategy:
      matrix:
        include:
          # - os: macos-latest
          #   java: 8
          # - os: windows-latest
          #   java: 8
          - os: ubuntu-latest
            java: 8
          - os: ubuntu-latest
            java: 11
      fail-fast: false
    runs-on: ${{ matrix.os }}
    steps:
      - name: Check out WALA sources
        uses: actions/checkout@v2
      - name: Cache Goomph
        uses: actions/cache@v1
        with:
          path: ~/.goomph
          key: ${{ runner.os }}-goomph-${{ hashFiles('build.gradle') }}
          restore-keys: ${{ runner.os }}-goomph-
      - name: 'Set up JDK ${{ matrix.java }}'
        uses: actions/setup-java@v2
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
      - name: Build and test using Gradle with ECJ
        uses: gradle/gradle-build-action@v2
        with:
          gradle-executable: xvfb-gradle.sh
          arguments: aggregatedJavadocs javadoc build lintGradle
        # testing ECJ compilation on any one OS is sufficient; we choose Linux arbitrarily
        if: runner.os == 'Linux'
      - name: Build and test using Gradle but without ECJ
        uses: gradle/gradle-build-action@v2
        with:
          arguments: aggregatedJavadocs javadoc build -PskipJavaUsingEcjTasks
        if: runner.os != 'Linux'
      - name: Check for Git cleanliness after build and test
        run: ./check-git-cleanliness.sh
        # not running in Borne or POSIX shell on Windows
        if: runner.os != 'Windows'
  build_maven:
    name: "Maven Eclipse Tests"
    strategy:
      matrix:
        include:
          - os: ubuntu-latest
            java: 8
      fail-fast: false
    runs-on: ${{ matrix.os }}
    steps:
      - name: Check out WALA sources
        uses: actions/checkout@v2
      - name: 'Cache local Maven repository'
        uses: actions/cache@v2
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-
      - name: 'Set up JDK ${{ matrix.java }}'
        uses: actions/setup-java@v2
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
      - name: 'Prep for Maven'
        shell: bash
        run: ./gradlew prepareMavenBuild publishToMavenLocal -x javadoc
      - name: 'Maven Tests'
        shell: bash
        run: mvn clean install -B -q -Dmaven.javadoc.skip=true
  generate_docs:
    name: 'Generate latest docs'
    needs: [build_gradle, build_maven]
    if: github.event_name == 'push' && github.repository == 'huantd210/WALA' && github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    steps:
      - name: 'Check out repository'
        uses: actions/checkout@v2
      - name: Cache Goomph
        uses: actions/cache@v1
        with:
          path: ~/.goomph
          key: ${{ runner.os }}-goomph-${{ hashFiles('build.gradle') }}
          restore-keys: ${{ runner.os }}-goomph-
      - name: 'Set up JDK 8'
        uses: actions/setup-java@v2
        with:
          java-version: 8
          distribution: 'temurin'
      - name: 'Generate latest docs'
        env:
          GITHUB_TOKEN: ${{ secrets.WALA_BOT_GH_TOKEN }}
        run: ./generate-latest-docs.sh
  publish_snapshot:
    name: 'Publish snapshot'
    needs: [build_gradle, build_maven]
    if: github.event_name == 'push' && github.repository == 'huantd210/WALA' && github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    steps:
      - name: 'Check out repository'
        uses: actions/checkout@v2
      - name: Cache Goomph
        uses: actions/cache@v1
        with:
          path: ~/.goomph
          key: ${{ runner.os }}-goomph-${{ hashFiles('build.gradle') }}
          restore-keys: ${{ runner.os }}-goomph-
      - name: 'Set up JDK 8'
        uses: actions/setup-java@v2
        with:
          java-version: 8
          distribution: 'temurin'
      - name: 'Publish'
        env:
          ORG_GRADLE_PROJECT_SONATYPE_NEXUS_USERNAME: ${{ secrets.SONATYPE_NEXUS_USERNAME }}
          ORG_GRADLE_PROJECT_SONATYPE_NEXUS_PASSWORD: ${{ secrets.SONATYPE_NEXUS_PASSWORD }}
        uses: gradle/gradle-build-action@v2
        with:
          arguments: publishAllPublicationsToMavenRepository
