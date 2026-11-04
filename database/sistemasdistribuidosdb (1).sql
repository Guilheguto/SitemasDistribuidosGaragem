-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Tempo de geração: 05/11/2026 às 00:15
-- Versão do servidor: 10.4.32-MariaDB
-- Versão do PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Banco de dados: `sistemasdistribuidosdb`
--

-- --------------------------------------------------------

--
-- Estrutura para tabela `active_sessions`
--

CREATE TABLE `active_sessions` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `username` varchar(20) NOT NULL,
  `token` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Despejando dados para a tabela `active_sessions`
--

INSERT INTO `active_sessions` (`id`, `username`, `token`, `created_at`) VALUES
(55, 'kauan', '40d5da00-5475-4571-abf6-b2d1d4f1331e', '2026-11-04 23:11:45');

-- --------------------------------------------------------

--
-- Estrutura para tabela `parking_spots`
--

CREATE TABLE `parking_spots` (
  `spot_name` varchar(4) NOT NULL,
  `floor_number` int(11) NOT NULL,
  `spot_type` varchar(10) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'disponivel',
  `license_plate` varchar(7) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Despejando dados para a tabela `parking_spots`
--

INSERT INTO `parking_spots` (`spot_name`, `floor_number`, `spot_type`, `status`, `license_plate`) VALUES
('A1', 1, 'combustao', 'ocupada', '1231231'),
('A2', 1, 'eletrico', 'disponivel', NULL);

-- --------------------------------------------------------

--
-- Estrutura para tabela `users`
--

CREATE TABLE `users` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `username` varchar(20) NOT NULL,
  `password` varchar(20) NOT NULL,
  `creation_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `name` varchar(255) NOT NULL,
  `is_admin` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Despejando dados para a tabela `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `creation_date`, `name`, `is_admin`) VALUES
(1, 'guilhermeadm', '!winner2026GQ', '2026-10-07 22:12:44', 'guilhereme', 1),
(3, 'jaaaoo', 'Luci1207!', '2026-10-07 23:18:06', 'joao', 0),
(4, 'guilhermeteste', '!winner2026GQ', '2026-10-07 23:20:00', 'guilherme', 0),
(5, 'joao', 'Luciani19281!', '2026-10-07 23:50:24', 'Joao', 0),
(6, 'joao123', 'Luci1207!', '2026-10-07 23:52:32', 'joaoaltevir', 0),
(17, 'admin', 'Admin123!', '2026-11-04 21:39:27', 'admin', 1),
(18, 'guilherme', '!winner2026GQ', '2026-11-04 21:44:36', 'guilherme', 0),
(28, 'thiago.dev', 'User123!', '2026-11-04 22:52:13', 'Thiago Augustim', 0),
(35, 'kayo', 'Kayo2006#', '2026-11-04 22:57:56', 'Kauan', 0),
(36, 'kauan', 'Kauan2008#', '2026-11-04 23:11:30', 'Peixe', 0);

-- --------------------------------------------------------

--
-- Estrutura para tabela `vehicles`
--

CREATE TABLE `vehicles` (
  `license_plate` varchar(7) NOT NULL,
  `owner_username` varchar(20) NOT NULL,
  `brand` varchar(20) NOT NULL,
  `model` varchar(20) DEFAULT NULL,
  `vehicle_type` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Despejando dados para a tabela `vehicles`
--

INSERT INTO `vehicles` (`license_plate`, `owner_username`, `brand`, `model`, `vehicle_type`) VALUES
('1231231', 'guilherme', 'Maquina', 'Mortifera', 'combustao'),
('1234567', 'kauan', 'Marca Legal', 'Modelo Legal', 'combustao'),
('1234568', 'Kayo', 'Marcal Legal', 'Modelo Legal', 'eletrico'),
('ABC1234', 'thiago.dev', 'Chevrolet', NULL, 'eletrico');

--
-- Índices para tabelas despejadas
--

--
-- Índices de tabela `active_sessions`
--
ALTER TABLE `active_sessions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `token` (`token`);

--
-- Índices de tabela `parking_spots`
--
ALTER TABLE `parking_spots`
  ADD PRIMARY KEY (`spot_name`),
  ADD UNIQUE KEY `license_plate` (`license_plate`);

--
-- Índices de tabela `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Índices de tabela `vehicles`
--
ALTER TABLE `vehicles`
  ADD PRIMARY KEY (`license_plate`),
  ADD KEY `idx_vehicles_owner` (`owner_username`);

--
-- AUTO_INCREMENT para tabelas despejadas
--

--
-- AUTO_INCREMENT de tabela `active_sessions`
--
ALTER TABLE `active_sessions`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=56;

--
-- AUTO_INCREMENT de tabela `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=37;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
