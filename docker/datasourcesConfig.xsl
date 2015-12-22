<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:dm="urn:jboss:domain:3.0"
                xmlns:ds="urn:jboss:domain:datasources:3.0"
				xmlns:ee="urn:jboss:domain:ee:3.0">

    <xsl:output method="xml" indent="yes" />

    <xsl:param name="orchestrator.DB.DN" />
    <xsl:param name="orchestrator.DB.name" />
    <xsl:param name="orchestrator.DB.user" />
	<xsl:param name="orchestrator.DB.pwd" />

<!--	<xsl:param name="orchestrator-test.DB.DN" />
    <xsl:param name="orchestrator-test.DB.name" />
    <xsl:param name="orchestrator-test.DB.user" />
	<xsl:param name="orchestrator-test.DB.pwd" /> -->

 	<xsl:variable name="newDatasourcesDefinition">
		<datasources xmlns="urn:jboss:domain:datasources:3.0">
			<xa-datasource jndi-name="java:jboss/datasources/orchestrator" pool-name="orchestratorDS" enabled="true" use-java-context="true" use-ccm="true">
				<xa-datasource-property name="URL">
					<xsl:value-of select="concat('jdbc:mysql://', $orchestrator.DB.DN, ':3306/', $orchestrator.DB.name)" />
				</xa-datasource-property>
				<xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</xa-datasource-class>
				<driver>mysql</driver>
				<xa-pool>
					<min-pool-size>1</min-pool-size>
					<max-pool-size>20</max-pool-size>
					<prefill>true</prefill>
				</xa-pool>
				<security>
					<user-name>
						<xsl:value-of select="$orchestrator.DB.user" />
					</user-name>
					<password>
						<xsl:value-of select="$orchestrator.DB.pwd" />
					</password>
				</security>
			</xa-datasource>
<!--			<xa-datasource jndi-name="java:jboss/datasources/orchestrator-test" pool-name="orchestratorTestDS" enabled="true" use-java-context="true" use-ccm="true">
				<xa-datasource-property name="URL">
					<xsl:value-of select="concat('jdbc:mysql://', $orchestrator-test.DB.DN, ':3306/', $orchestrator-test.DB.name)" />
				</xa-datasource-property>
				<xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</xa-datasource-class>
				<driver>mysql</driver>
				<xa-pool>
					<min-pool-size>1</min-pool-size>
					<max-pool-size>20</max-pool-size>
					<prefill>true</prefill>
				</xa-pool>
				<security>
					<user-name>
						<xsl:value-of select="$orchestrator-test.DB.user" />
					</user-name>
					<password>
						<xsl:value-of select="$orchestrator-test.DB.pwd" />
					</password>
				</security>
			</xa-datasource> -->
			<drivers>
                <driver name="mysql" module="com.mysql">
					<xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</xa-datasource-class>
                </driver>
            </drivers>
		</datasources>
	</xsl:variable>


    <xsl:template match="//ds:subsystem/ds:datasources">
        <xsl:copy-of copy-namespaces="no" select="$newDatasourcesDefinition"/>
    </xsl:template>
	
    <xsl:template match="//ee:subsystem/ee:default-bindings/@datasource">
		<xsl:attribute name="datasource">java:jboss/datasources/orchestrator</xsl:attribute>
    </xsl:template>
	
    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>