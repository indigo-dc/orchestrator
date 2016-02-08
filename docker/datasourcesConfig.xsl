<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:dm="urn:jboss:domain:3.0"
                xmlns:ds="urn:jboss:domain:datasources:3.0"
				xmlns:ee="urn:jboss:domain:ee:3.0">

    <xsl:output method="xml" indent="yes" />

    <xsl:param name="orchestrator.DB.endpoint" />
    <xsl:param name="orchestrator.DB.name" />
    <xsl:param name="orchestrator.DB.user" />
	<xsl:param name="orchestrator.DB.pwd" />
	<xsl:param name="workflow.DB.endpoint" />
    <xsl:param name="workflow.DB.name" />
    <xsl:param name="workflow.DB.user" />
	<xsl:param name="workflow.DB.pwd" />

 	<xsl:variable name="newDatasourcesDefinition">
		<datasources xmlns="urn:jboss:domain:datasources:3.0">
			<xa-datasource jndi-name="java:jboss/datasources/orchestrator" pool-name="orchestratorDS" enabled="true" use-java-context="true" use-ccm="true">
				<xa-datasource-property name="URL">
					<xsl:value-of select="concat('jdbc:mysql://', $orchestrator.DB.endpoint, '/', $orchestrator.DB.name)" />
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
			<xa-datasource jndi-name="java:jboss/datasources/workflow" pool-name="workflowDS" enabled="true" use-java-context="true" use-ccm="true">
				<xa-datasource-property name="URL">
					<xsl:value-of select="concat('jdbc:mysql://', $workflow.DB.endpoint, '/', $workflow.DB.name)" />
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
						<xsl:value-of select="$workflow.DB.user" />
					</user-name>
					<password>
						<xsl:value-of select="$workflow.DB.pwd" />
					</password>
				</security>
			</xa-datasource>
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