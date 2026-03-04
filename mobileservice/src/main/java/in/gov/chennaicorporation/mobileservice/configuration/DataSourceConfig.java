package in.gov.chennaicorporation.mobileservice.configuration;

import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

	private static String host = "localhost:3306";
	private static String dbpassword = "root";
	// private static String dbpassword = "gccroot";

	// AWS
	// private static String host =
	// "gcc-facial-db-instance-1.cf48eqcciziq.ap-south-1.rds.amazonaws.com:3306";
	// private static String dbpassword = "gcc-facial-password";

	////////////////////////////// (For GCC APP) ////////////////////////
	@Configuration
	@EnableTransactionManagement
	@EnableJpaRepositories(entityManagerFactoryRef = "appEntityManagerFactory", transactionManagerRef = "appTransactionManager", basePackages = {
			"in.gov.chennaicorporation" })

	public static class MysqlAppDataSourceConfig {
		@Primary
		@Bean(name = "mysqlAppDataSource")
		public DataSource mysqlAppDataSource() {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
			dataSource.setUrl("jdbc:mysql://" + host + "/gcc_apps");
			dataSource.setUsername("root");
			dataSource.setPassword(dbpassword);
			return dataSource;
		}

		@Primary
		@Bean(name = "appEntityManagerFactory")
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(
				@Qualifier("mysqlAppDataSource") DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
			entityManagerFactoryBean.setDataSource(dataSource);
			entityManagerFactoryBean.setPackagesToScan("in.gov.chennaicorporation");
			entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
			Properties properties = new Properties();
			properties.setProperty("hibernate.hbm2ddl.auto", "update");
			entityManagerFactoryBean.setJpaProperties(properties);

			return entityManagerFactoryBean;
		}

		@Primary
		@Bean(name = "appTransactionManager")
		public PlatformTransactionManager transactionManager(
				@Qualifier("appEntityManagerFactory") jakarta.persistence.EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}

	////////////////////////////// (For GCC Activity Master)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlActivityDataSource")
	public DataSource mysqlActivityDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_activity");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Penalty Master) ////////////////////////
	@Bean(name = "mysqlPenaltyDataSource")
	public DataSource mysqlPenaltyDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_penalty_pos");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Cattle Catch Master)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlCattleCatchDataSource")
	public DataSource mysqlCattleCatchDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_penalty");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Cattle Catch Master)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlHoardingsDataSource")
	public DataSource mysqlHoardingsDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_penalty_hoardings");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For PGR Master) ////////////////////////
	@Bean(name = "mysqlPGRMasterDataSource")
	public DataSource mysqlPGRMasterDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/erp_pgr");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For PGR Master) ////////////////////////
	@Bean(name = "mysqlGCCUserDataSource")
	public DataSource mysqlGCCUserDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_users");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For PGR Master) ////////////////////////
	@Bean(name = "mysqlGccSOSDataSource")
	public DataSource mysqlGccSOSDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_sos");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For NULM Staff) ////////////////////////
	@Bean(name = "mysqlNulmDataSource")
	public DataSource mysqlNulmDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_nulm");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Stray Dog (veterinary))
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlGccStraydogsDataSource")
	public DataSource mysqlGccStraydogsDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_stray_dogs");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Schools) ////////////////////////
	@Bean(name = "mysqlGccSchoolsDataSource")
	public DataSource mysqlGccSchoolsDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_schools");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Parks and Playground)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlGccParksDataSource")
	public DataSource mysqlGccParksDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_parks_playground");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Toilets) ////////////////////////
	@Bean(name = "mysqlToiletsDataSource")
	public DataSource mysqlToiletsDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_toilets");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For C&D Waste) ////////////////////////
	@Bean(name = "mysqlC_D_WasteDataSource")
	public DataSource mysqlC_D_WasteDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_c_d_waste");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Schools) ////////////////////////
	@Bean(name = "mysqlC_D_WasteUserDataSource")
	public DataSource mysqlC_D_WasteUserDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_c_d_waste_user");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Schools) ////////////////////////
	@Bean(name = "mysqlMosquitoDataSource")
	public DataSource mysqlMosquitoDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_mosquito_survey");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Works) ////////////////////////
	@Bean(name = "mysqlWorksDataSource")
	public DataSource mysqlWorksDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_works_status");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Street Vendor`s) ////////////////////////
	@Bean(name = "mysqlGccStreetVendorDataSource")
	public DataSource mysqlGccStreetVendorDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_street_vendor");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Street Vendor`s) ////////////////////////
	@Bean(name = "mysqlGccEducationDataSource")
	public DataSource mysqlGccEducationDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_schools_web");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Street Vendor`s) ////////////////////////
	@Bean(name = "mysqlBusShelterDataSource")
	public DataSource mysqlBusShelterDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_bus_shelters");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For domestic_waste_management )
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlGccDomesticWasteManagementDataSource")
	public DataSource domesticwastemanagement() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/domestic_waste_management");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For API Application Log)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlAPIDataSource")
	public DataSource mysqlAPIDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_api_access_logs");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Toilets) ////////////////////////
	@Bean(name = "mysqlPrivateToiletsDataSource")
	public DataSource mysqlPrivateToiletsDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_private_toilets");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Toilets) ////////////////////////
	@Bean(name = "mysqlRoadCutDataSource")
	public DataSource mysqlRoadCutDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_roadcut");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For PotHole) ////////////////////////
	@Bean(name = "mysqlGccPotHoleDataSource")
	public DataSource mysqlGccPotHoleDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_pothole");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Construction Guidelines)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlGccConstructionGuidelinesSource")
	public DataSource mysqlGccConstructionGuidelinesSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_construction_guidelines");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For View Cutter) ////////////////////////
	@Bean(name = "mysqlViewCutterDataSource")
	public DataSource mysqlViewCutterDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_view_cutter");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For SpeedBrake ) ////////////////////////
	@Bean(name = "mysqlGccSpeedBrakeSource")
	public DataSource mysqlGccSpeedBrakeSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_speedbrake");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For Construction Guidelines)
	////////////////////////////// ////////////////////////
	@Bean(name = "mysqlGccBuildingDemolitionSource")
	public DataSource mysqlGccBuildingDemolitionSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_building_demolition");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Construction Guidelines)
	///////////////////////////// ////////////////////////
	@Bean(name = "mysqlGccRoadWarSource")
	public DataSource mysqlGccRoadWarSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_roadwar");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For ManHole) ////////////////////////
	@Bean(name = "mysqlGccManHoleSource")
	public DataSource mysqlGccManHoleSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_manholes");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Disposal Point) ////////////////////////
	@Bean(name = "mysqlGccDisposalPointSource")
	public DataSource mysqlGccDisposalPointSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_disposalpoint");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Street Cleaning) ////////////////////////
	@Bean(name = "mysqlGccStreetCleaningSource")
	public DataSource mysqlGccStreetCleaningSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_street_cleaning");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Pumps & Sumps) ////////////////////////
	@Bean(name = "mysqlGccPumpSource")
	public DataSource mysqlGccPumpSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_pumps");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Special Vehicles) ////////////////////////
	@Bean(name = "mysqlGccSpecialVehicleSource")
	public DataSource mysqlGccSpecialVehicleSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_special_vehicle");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Sluice Point) ////////////////////////
	@Bean(name = "mysqlGccSluicePointSource")
	public DataSource mysqlGccSluicePointSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_sluicepoint");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Railway Culverts) ////////////////////////
	@Bean(name = "mysqlGccRailwayCulvertSource")
	public DataSource mysqlGccRailwayCulvertSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_railway_culvert");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	///////////////////////////// (For Food Distribution) ////////////////////////
	@Bean(name = "mysqlGccFoodDistributionSource")
	public DataSource mysqlGccFoodDistributionSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_food_distribution");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For FlagPole) ////////////////////////
	@Bean(name = "mysqlGccFlagPoleDataSource")
	public DataSource mysqlGccFlagPoleDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_flag_pole");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Medical Camp ////////////////////////
	@Bean(name = "mysqlGccMedicalCampDataSource")
	public DataSource mysqlGccMedicalCampDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_health_camp");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Home Less ////////////////////////
	@Bean(name = "mysqlGccHomelessDataSource")
	public DataSource mysqlGccHomelessDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/homeless_survey");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Abandoned Vehicle ////////////////////////
	@Bean(name = "mysqlGccAbandonedVehicleSource")
	public DataSource mysqlGccAbandonedVehicleSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/abandoned_vehicle");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Green Committee ////////////////////////
	@Bean(name = "mysqlGccGreenCommitteeSource")
	public DataSource mysqlGccGreenCommitteeSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/green_committee");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Thooimai Mission ////////////////////////
	@Bean(name = "mysqlThooimaiMissionSource")
	public DataSource mysqlThooimaiMissionSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/thooimai_mission");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Boat Service ////////////////////////
	@Bean(name = "mysqlBoatSource")
	public DataSource mysqlBoatSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/boat_service");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// (For V-Track (jTrack)) ////////////////////////
	@Bean(name = "mysqlVehiclTrackingDataSource")
	public DataSource mysqlVehiclTrackingDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_vtrack");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Abandoned Vehicle ////////////////////////
	@Bean(name = "mysqlillegalDebrisVehicleSource")
	public DataSource mysqlillegalDebrisVehicleSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/illegal_debris_vehicle");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For Abandoned Vehicle ////////////////////////
	@Bean(name = "mysqlElectionDataSource")
	public DataSource mysqlElectionDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_election");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// For IE Complaints ////////////////////////
	@Bean(name = "mysqlIEComplaintsDataSource")
	public DataSource mysqlIEComplaintsDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + "/gcc_ie_complaints");
		dataSource.setUsername("root");
		dataSource.setPassword(dbpassword);
		return dataSource;
	}

	////////////////////////////// ORACLE ////////////////////////
	@Bean(name = "oracleERPDataSource")
	public DataSource oracleERPDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
		dataSource.setUrl("jdbc:oracle:thin:@10.1.0.14:1521:chncorp");
		dataSource.setUsername("FAS");
		dataSource.setPassword("FAS");
		return dataSource;
	}

}